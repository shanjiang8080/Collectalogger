package com.example.collectalogger2.util.libraryObjects

import android.util.Log
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.APIStatusException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Thrown when the saved portal cookies are no longer valid (the portal page
 * no longer contains the virtual game card data). Data sources translate this
 * to an AccountExpiryException so the app can prompt a re-login.
 */
class NintendoSessionExpiredException(message: String) : APIException(message)

/**
 * Thrown when the Virtual Game Cards GraphQL request fails. languageFallback
 * tells the caller whether retrying with a different language is worthwhile
 * (Nintendo returns a bare "error" without a code for unsupported locales).
 */
class NintendoGraphQLRequestException(message: String, val languageFallback: Boolean) :
    APIException(message)

/**
 * API client for the Nintendo Account Virtual Game Cards web portal.
 * Modeled on Playnite's NintendoLibrary extension by XenorPLxx:
 * https://github.com/XenorPLxx/playnite-library-nintendo
 *
 * There is no official Nintendo API, so this scrapes Nintendo's consumer
 * web portal. The flow is:
 * 1. The user logs into the account portal in a WebView (PORTAL_URL). The
 *    login overlay captures the page's session cookies.
 * 2. The portal page HTML is fetched with those cookies; it embeds the
 *    Savanna eShop GraphQL credentials (idToken, savannaClientId,
 *    shopGraphQLApiUrl) and the account's locale in data-json blobs.
 * 3. Owned games are listed by paging the getVgcs GraphQL query, one
 *    Virtual Game Card per owned title.
 *
 * Since this is a consumer web page, portal redesigns or locale changes
 * can break the parsing below. The language handling mirrors the plugin's
 * findings: GraphQL requests fail with a bare "error" for unsupported
 * locales, so requests fall back through language candidates.
 */
object NintendoSource {
    private const val TAG = "NintendoSource"

    /**
     * The Virtual Game Cards portal page. The login overlay loads this URL and
     * considers the login successful once the browser lands back on it.
     */
    const val PORTAL_URL =
        "https://accounts.nintendo.com/portal/vgcs/?sort=activated_date&order=desc"

    // The Nintendo Account portal domain the cookies are captured from
    const val PORTAL_COOKIE_URL = "https://accounts.nintendo.com"
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/138.0.0.0 Safari/537.36"
    /**
     * Mobile user agent for the login WebView: Nintendo's account pages render
     * their mobile layout for this, which fits the phone-sized login dialog.
     * Only the overlay uses this; the portal fetching below keeps the desktop
     * user agent (what Playnite's plugin uses, so the same page is served).
     */
    const val MOBILE_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/138.0.0.0 Mobile Safari/537.36"

    private const val PAGE_REQUEST_LIMIT = 300
    // Nintendo's portal uses this fixed shop context for virtual game cards
    private const val OFF_DEVICE_SHOP_ID = 3

    private val client by lazy { HttpClient(Android) }

    /**
     * A queried title from the Virtual Game Cards list.
     * isSwitch/isSwitch2 are based on the game's platform flags: "NX" is the
     * Switch, "OUNCE" is Nintendo's internal codename for the Switch 2.
     */
    data class VgcTitle(
        val applicationId: String,
        val name: String,
        val isSwitch: Boolean,
        val isSwitch2: Boolean,
        val hasApplication: Boolean,
        val hasAddOnContents: Boolean
    )

    /**
     * The credentials + locale scraped from the portal page, needed to query
     * the virtual game card GraphQL API.
     */
    data class VgcPortalParams(
        val idToken: String,
        val savannaClientId: String,
        val shopGraphQLApiUrl: String,
        val country: String,
        val languageCode: String,
        val portalLanguage: String,
        val nasLanguage: String
    )

    /**
     * Fetches all of the account's virtual game cards with the given cookies
     * (a JSON object mapping base URLs to raw cookie header strings, as saved
     * by the login overlay). Returns one VgcTitle per unique application id.
     */
    suspend fun getVirtualGameCards(cookieInfo: String): List<VgcTitle> {
        val cookieHeader = JSONObject(cookieInfo).optString(PORTAL_COOKIE_URL, "")
        if (cookieHeader == "") {
            throw NintendoSessionExpiredException(
                "The saved Nintendo portal cookies are missing the $PORTAL_COOKIE_URL entry!"
            )
        }

        val portalResponse = getPortalPage(cookieHeader)
        val portalParams = parsePortalParams(portalResponse) ?: run {
            Log.w(TAG, "The Nintendo portal page did not contain virtual game card data!")
            throw NintendoSessionExpiredException(
                "The Nintendo portal page did not contain virtual game card data. " +
                "The saved session is expired or invalid; please log in again!"
            )
        }

        // Try the mapped language first, then English fallbacks (like Playnite's plugin)
        val languageCandidates = listOfNotNull(
            portalParams.nasLanguage,
            getNasLanguage(portalParams.portalLanguage),
            "en-GB",
            "en-US"
        ).distinct()

        var lastException: NintendoGraphQLRequestException? = null
        for ((index, nasLanguage) in languageCandidates.withIndex()) {
            // Only retry with English candidates after the primary attempt
            if (index > 0 && isEnglishFallback(portalParams.nasLanguage, nasLanguage)) {
                Log.w(
                    TAG,
                    "Retrying Nintendo virtual game cards request with fallback language $nasLanguage"
                )
            }
            val params = portalParams.copy(nasLanguage = nasLanguage)
            try {
                val views = fetchAllVgcViews(params)
                return views.mapNotNull { parseVgcTitle(it) }
            } catch (e: NintendoGraphQLRequestException) {
                lastException = e
                if (!e.languageFallback) break
            }
        }
        throw lastException
            ?: APIException("The Nintendo virtual game cards request failed for unknown reasons!")
    }

    // True when the candidate is one of the English fallback languages used
    // after a mapped language attempt
    private fun isEnglishFallback(primary: String, candidate: String): Boolean {
        if (candidate != "en-GB" && candidate != "en-US") return false
        return primary != "en-GB" && primary != "en-US"
    }

    // --- Play activity (Nintendo Store app / znej API) ---
    //
    // The Virtual Game Cards portal provides the owned-game list but no
    // playtime. The Nintendo Store app's play activity API does, with the
    // complete history (titleId, firstPlayedAt, lastPlayedAt,
    // totalPlayedDays, totalPlayedMinutes). Reverse-engineered from the app
    // (com.nintendo.znej), see the spike in spikes/switch_playtime_spike.ps1:
    //
    // 1. The user logs in through the OAuth authorize endpoint with a PKCE
    //    challenge; the "Select this account" button navigates to
    //    npf<client id>://auth#session_token_code=... (intercepted by the
    //    login overlay).
    // 2. The session_token_code + verifier are exchanged for a
    //    session_token, which is valid for about two years.
    // 3. The session_token is exchanged for a 15-minute access_token, which
    //    authorizes the play_histories GET request.

    // The client id of the Nintendo Store app (com.nintendo.znej)
    private const val ZNEJ_CLIENT_ID = "5c38e31cd085304b"
    private const val ZNEJ_REDIRECT_URI = "npf$ZNEJ_CLIENT_ID://auth"
    // User agent the Nintendo Store app uses for its API calls
    private const val ZNEJ_USER_AGENT = "com.nintendo.znej/3.3.1 (Android/14)"

    /**
     * A play history entry from the Nintendo Store app play activity API.
     * isSwitch2 comes from the entry's platform field ("HAC" is the Switch
     * hardware, "BEE" the Switch 2).
     */
    data class NintendoPlaytime(
        val titleId: String,
        val name: String,
        val minutes: Long,
        val isSwitch2: Boolean
    )

    /**
     * Generates a PKCE code verifier, a random string of 45 chars.
     */
    fun generateCodeVerifier(): String {
        val randomStringChars = "ABCDEFGHIJKLMNOPQRSTYVWXZabcdefghijklmnopqrstyvwxz0123456789_"
        val result = StringBuilder(45)
        repeat(45) {
            result.append(randomStringChars[kotlin.random.Random.nextInt(0, randomStringChars.length - 1)])
        }
        return result.toString()
    }

    /**
     * Generates the S256 code challenge from the code verifier: the SHA-256
     * hash of the verifier, base64url-encoded without padding.
     */
    fun getCodeChallenge(codeVerifier: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(Charsets.UTF_8))
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * The Nintendo Account login URL for the play activity API. The login
     * overlay loads this in a WebView and intercepts the redirect to
     * ZNEJ_REDIRECT_URI carrying the session_token_code.
     */
    fun getAuthorizeUrl(codeChallenge: String): String {
        return "https://accounts.nintendo.com/connect/1.0.0/authorize" +
            "?client_id=$ZNEJ_CLIENT_ID" +
            "&redirect_uri=${URLEncoder.encode(ZNEJ_REDIRECT_URI, "UTF-8")}" +
            "&response_type=session_token_code" +
            "&scope=${URLEncoder.encode("openid user user.mii user.email user.links[].id", "UTF-8")}" +
            "&session_token_code_challenge=$codeChallenge" +
            "&session_token_code_challenge_method=S256" +
            "&state=collectalogger" +
            "&theme=login_form"
    }

    /**
     * Exchanges the session_token_code captured from the login redirect
     * (plus the code verifier used for the challenge) for a session_token,
     * which is valid for about two years. Throws a
     * NintendoSessionExpiredException when the code is invalid or expired.
     */
    suspend fun getSessionToken(sessionTokenCode: String, codeVerifier: String): String {
        val body = "client_id=$ZNEJ_CLIENT_ID" +
            "&session_token_code=${
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(sessionTokenCode, "UTF-8")
                }
            }" +
            "&session_token_code_verifier=${
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(codeVerifier, "UTF-8")
                }
            }"
        val responseText = try {
            val response = client.post("https://accounts.nintendo.com/connect/1.0.0/api/session_token") {
                header("User-Agent", ZNEJ_USER_AGENT)
                header("Accept", "application/json")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                // A 400 means the session token code is invalid or expired
                if (response.status.value == 400) {
                    throw NintendoSessionExpiredException(
                        "The Nintendo login code was rejected ($errorText); please log in again!"
                    )
                }
                throw APIStatusException(
                    "Nintendo session token exchange failed with HTTP status ${response.status.value}: $errorText",
                    response.status.value
                )
            }
            response.bodyAsText()
        } catch (ex: APIException) {
            throw ex
        } catch (ex: Exception) {
            Log.e(TAG, "Nintendo session token exchange failed!", ex)
            throw APIException("Nintendo session token exchange failed: ${ex.message}")
        }
        return JSONObject(responseText).getString("session_token")
    }

    /**
     * Fetches the account's complete play history with the given
     * session_token (which gets exchanged for a short-lived access_token
     * first). Throws a NintendoSessionExpiredException when the session
     * token is no longer valid.
     */
    suspend fun getPlayHistories(sessionToken: String): List<NintendoPlaytime> {
        // Exchange the session token for an access token
        val tokenBody = JSONObject()
            .put("client_id", ZNEJ_CLIENT_ID)
            .put("session_token", sessionToken)
            .put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer-session-token")
        val tokenResponseText = try {
            val response = client.post("https://accounts.nintendo.com/connect/1.0.0/api/token") {
                header("User-Agent", ZNEJ_USER_AGENT)
                contentType(ContentType.Application.Json)
                setBody(tokenBody.toString())
            }
            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                // A 400 means the session token is invalid or expired
                if (response.status.value == 400) {
                    throw NintendoSessionExpiredException(
                        "Cannot refresh the Nintendo play activity login! Must log in again to Nintendo!"
                    )
                }
                throw APIStatusException(
                    "Nintendo access token request failed with HTTP status ${response.status.value}: $errorText",
                    response.status.value
                )
            }
            response.bodyAsText()
        } catch (ex: APIException) {
            throw ex
        } catch (ex: Exception) {
            Log.e(TAG, "Nintendo access token request failed!", ex)
            throw APIException("Nintendo access token request failed: ${ex.message}")
        }
        val accessToken = JSONObject(tokenResponseText).getString("access_token")

        // Fetch the play histories
        val responseText = try {
            val response = client.get("https://app-api.znej.nintendo.com/api/v2.0/users/me/play_histories") {
                header("User-Agent", ZNEJ_USER_AGENT)
                header("Authorization", "Bearer $accessToken")
                header("gentry-locale", "en-US")
                header("Accept", "application/json")
            }
            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                // 401/403 mean the access token was rejected (stale session)
                if (response.status.value == 401 || response.status.value == 403) {
                    throw NintendoSessionExpiredException(
                        "Cannot access the Nintendo play activity! Must log in again to Nintendo!"
                    )
                }
                throw APIStatusException(
                    "Nintendo play histories request failed with HTTP status ${response.status.value}: $errorText",
                    response.status.value
                )
            }
            response.bodyAsText()
        } catch (ex: APIException) {
            throw ex
        } catch (ex: Exception) {
            Log.e(TAG, "Nintendo play histories request failed!", ex)
            throw APIException("Nintendo play histories request failed: ${ex.message}")
        }
        return parsePlayHistories(responseText)
    }

    /**
     * Parses the play_histories response into NintendoPlaytime entries.
     */
    fun parsePlayHistories(body: String): List<NintendoPlaytime> {
        val root = JSONObject(body)
        val historiesJson = root.optJSONArray("playHistories") ?: JSONArray()
        val histories = mutableListOf<NintendoPlaytime>()
        for (i in 0 until historiesJson.length()) {
            val entry = historiesJson.getJSONObject(i)
            val titleId = entry.optString("titleId", "")
            if (titleId == "") continue
            histories.add(
                NintendoPlaytime(
                    titleId = titleId,
                    name = entry.optString("titleName", ""),
                    minutes = entry.optLong("totalPlayedMinutes", 0),
                    // "BEE" is the Switch 2; "HAC" (and anything else) the Switch
                    isSwitch2 = entry.optString("platform", "") == "BEE"
                )
            )
        }
        return histories
    }

    // Demos and test releases from the play history are never real owned games
    private val demoEntryRegex = Regex("""\b(demo|testpunch|testfire)\b""", RegexOption.IGNORE_CASE)
    // Non-game apps that show up in the play history
    private val nonGameEntries = setOf(
        "youtube", "twitch", "hulu", "niconico",
        "pokemon home", "pokémon home",
        "nintendo switch online"
    )
    // The Nintendo Classics launcher apps end with this suffix
    private const val classicsSuffix = "nintendo classics"

    /**
     * Whether a play history entry is a playable game (as opposed to demo
     * releases, streaming apps, or the Nintendo Classics launcher apps).
     * Used when importing played titles that are not in the owned library.
     */
    fun isPlayableGameEntry(name: String): Boolean {
        if (demoEntryRegex.containsMatchIn(name)) return false
        val cleaned = name.trim().lowercase()
        if (cleaned in nonGameEntries) return false
        if (cleaned.endsWith(classicsSuffix)) return false
        return true
    }

    /**
     * Pages through the virtual game card GraphQL API and returns the raw view
     * JSONObjects (all pages concatenated).
     */
    private suspend fun fetchAllVgcViews(params: VgcPortalParams): List<JSONObject> {
        val views = mutableListOf<JSONObject>()
        var offset = 0
        do {
            val page = getVgcPage(params, offset)
            views.addAll(page.views)
            offset += PAGE_REQUEST_LIMIT
        } while (offset < page.total)
        return views
    }

    /**
     * Returns the portal page HTML for the given cookie header string.
     */
    private suspend fun getPortalPage(cookieHeader: String): String {
        try {
            val response = client.get(PORTAL_URL) {
                header("User-Agent", USER_AGENT)
                header("Cookie", cookieHeader)
                header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            }
            return response.bodyAsText()
        } catch (ex: APIException) {
            throw ex
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to fetch the Nintendo portal page!", ex)
            throw APIException("Failed to fetch the Nintendo portal page: ${ex.message}")
        }
    }

    // The getVgcs GraphQL query (copied from Playnite's NintendoLibrary)
    private const val VGC_GRAPHQL_QUERY = """query getVgcs(
                  ${'$'}idToken: String!
                  ${'$'}country: CountryCode!
                  ${'$'}language: LanguageCode!
                  ${'$'}shopId: Int!
                  ${'$'}limit: Int!
                  ${'$'}nasLanguage: String!
                  ${'$'}offset: Int!
                  ${'$'}order: RequestableVgcViewOrder!
                  ${'$'}sortBy: RequestableVgcViewSortBy!
                  ${'$'}vgcViewType: VgcViewTypeInput
                  ${'$'}vgcViewStatus: VgcViewStatusInput
                ) @inContext(country: ${'$'}country, language: ${'$'}language, shopId: ${'$'}shopId) {
                  account {
                    vgc {
                      vgcViews: vgcViewsV2(
                        idToken: ${'$'}idToken,
                        limit: ${'$'}limit,
                        language: ${'$'}nasLanguage,
                        offset: ${'$'}offset,
                        order: ${'$'}order,
                        sortBy: ${'$'}sortBy,
                        isHidden: false,
                        vgcViewType: ${'$'}vgcViewType,
                        vgcViewStatus: ${'$'}vgcViewStatus,
                      ) {
                        offsetInfo {
                          total
                        }
                        views {
                          id
                          applicationId
                          applicationName
                          apparentPlatform
                          publisher
                          icon {
                            url
                          }
                          ownerNaId
                          userNaId
                          isHidden
                          isLending
                          isPartialLending
                          lendingExpireDatetime
                          insertedNsDeviceId
                          hasApplication: hasReleasedApplication
                          hasAddOnContents: hasReleasedAddOnContents
                          hasUpgrade: hasReleasedUpgrade
                          containsReleased
                        }
                      }
                    }
                  }
                }"""

    /**
     * Requests one page of virtual game cards. Returns the page as a
     * [VgcPage]; throws a [NintendoGraphQLRequestException] on GraphQL errors
     * (languageFallback marks an unsupported-locale error worth retrying).
     */
    internal data class VgcPage(val views: List<JSONObject>, val total: Int)

    private suspend fun getVgcPage(params: VgcPortalParams, offset: Int): VgcPage {
        val variables = JSONObject()
        variables.put("country", params.country)
        variables.put("idToken", params.idToken)
        variables.put("language", params.languageCode)
        variables.put("limit", PAGE_REQUEST_LIMIT)
        variables.put("nasLanguage", params.nasLanguage)
        variables.put("offset", offset)
        variables.put("order", "ASC")
        variables.put("shopId", OFF_DEVICE_SHOP_ID)
        variables.put("sortBy", "ACTIVATED_DATE")

        val body = JSONObject()
        body.put("query", VGC_GRAPHQL_QUERY)
        body.put("variables", variables)

        val responseText = try {
            val response = client.post(params.shopGraphQLApiUrl) {
                header("User-Agent", USER_AGENT)
                headers {
                    append("x-nintendo-savanna-client-id", params.savannaClientId)
                }
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                throw APIStatusException(
                    "Nintendo virtual game cards request to ${params.shopGraphQLApiUrl} failed " +
                    "with HTTP status ${response.status.value}: $errorText",
                    response.status.value
                )
            }
            response.bodyAsText()
        } catch (ex: APIException) {
            throw ex
        } catch (ex: Exception) {
            Log.e(TAG, "Nintendo virtual game cards request failed!", ex)
            throw APIException("Nintendo virtual game cards request failed: ${ex.message}")
        }

        return parseVgcPage(responseText)
    }

    // --- Pure parsing helpers (unit-tested without the network layer) ---

    /**
     * Parses one GraphQL response page. Throws a NintendoGraphQLRequestException
     * on GraphQL errors, with languageFallback=true when all errors look like
     * unsupported-locale errors (bare "error" messages without a code).
     */
    internal fun parseVgcPage(body: String): VgcPage {
        val root = JSONObject(body)
        val errors = root.optJSONArray("errors")
        if (errors != null && errors.length() > 0) {
            val messages = mutableListOf<String>()
            var languageFallback = true
            for (i in 0 until errors.length()) {
                val error = errors.getJSONObject(i)
                val message = error.optString("message", "Unknown error")
                messages.add(message)
                if (message != "error" || error.optJSONObject("extensions")?.has("code") == true) {
                    languageFallback = false
                }
            }
            throw NintendoGraphQLRequestException(
                "Nintendo virtual game cards request failed: ${messages.joinToString("; ")}",
                languageFallback
            )
        }

        val vgcViews = try {
            root.getJSONObject("data")
                .getJSONObject("account")
                .getJSONObject("vgc")
                .getJSONObject("vgcViews")
        } catch (_: Exception) {
            throw APIException("Nintendo returned an incomplete virtual game cards response!")
        }
        val viewsJson = vgcViews.optJSONArray("views") ?: JSONArray()
        val views = mutableListOf<JSONObject>()
        for (i in 0 until viewsJson.length()) {
            views.add(viewsJson.getJSONObject(i))
        }
        return VgcPage(views = views, total = vgcViews.getJSONObject("offsetInfo").getInt("total"))
    }

    /**
     * Parses the credentials and locale out of the portal page HTML. The page
     * embeds JSON blobs in <div id="data|meta|state" data-json="...">.
     * Returns null if the page doesn't contain them (session expired/login page).
     */
    fun parsePortalParams(html: String): VgcPortalParams? {
        val dataJson = parsePortalDataJson(html, "data") ?: return null
        val metaJson = parsePortalDataJson(html, "meta")
        val stateJson = parsePortalDataJson(html, "state")

        val idToken = dataJson.optString("idToken", "")
        val savannaClientId = dataJson.optString("savannaClientId", "")
        val shopGraphQLApiUrl = dataJson.optString("shopGraphQLApiUrl", "")
        if (idToken == "" || savannaClientId == "" || shopGraphQLApiUrl == "") return null

        // The state blob holds the account's country id and UI language
        val stateLang = stateJson?.optString("lang", "") ?: ""
        val countryId = stateJson?.optJSONObject("user")?.optInt("countryId", -1) ?: -1
        val country = metaJson?.optJSONArray("countries")
            ?.let { countries ->
                for (i in 0 until countries.length()) {
                    val entry = countries.getJSONObject(i)
                    if (entry.optInt("id") == countryId) return@let entry.optString("code")
                }
                ""
            }
            ?: ""
        if (country == "" || stateLang.length < 2) return null

        return VgcPortalParams(
            idToken = idToken,
            savannaClientId = savannaClientId,
            shopGraphQLApiUrl = shopGraphQLApiUrl,
            country = country,
            languageCode = stateLang.substring(0, 2),
            portalLanguage = stateLang,
            nasLanguage = getNasLanguage(stateLang) ?:
                // Fall back to the portal's own language for untranslated locales
                stateLang.substring(0, 2)
        )
    }

    /**
     * Extracts and HTML-unescapes the JSON blob embedded in a portal page div.
     */
    fun parsePortalDataJson(html: String, elementId: String): JSONObject? {
        val match = Regex("<div id=\"$elementId\" data-json=\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)
            .find(html) ?: return null
        return try {
            val decoded = match.groupValues[1]
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&#x27;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
            JSONObject(decoded)
        } catch (_: Exception) {
            Log.w(TAG, "Failed to parse the '$elementId' data-json blob of the Nintendo portal page!")
            null
        }
    }

    /**
     * Converts a portal UI language (e.g. "de-DE") to the NAS language
     * Nintendo's backend expects (e.g. "de"). Matches the mapping in
     * Nintendo's account/js/common.js, as found by Playnite's plugin.
     */
    fun getNasLanguage(portalLanguage: String): String? {
        return when (portalLanguage.lowercase()) {
            "en-us" -> "en-US"
            "en-gb" -> "en-GB"
            "de", "de-de" -> "de"
            "nl", "nl-nl" -> "nl"
            "ja", "ja-jp" -> "ja"
            "fr", "fr-fr" -> "fr"
            "es", "es-es" -> "es"
            "it", "it-it" -> "it"
            "pt", "pt-pt" -> "pt"
            "ru", "ru-ru" -> "ru"
            "ko", "ko-kr" -> "ko"
            "es-mx" -> "es-419"
            "fr-ca" -> "fr-CA"
            "pt-br" -> "pt-BR"
            "zh-tw" -> "zh-Hant"
            else -> null
        }
    }

    /**
     * Parses a raw virtual game card view into a VgcTitle, or null if it
     * doesn't represent a playable application.
     */
    fun parseVgcTitle(view: JSONObject): VgcTitle? {
        val applicationId = view.optString("applicationId", "")
        val name = view.optString("applicationName", "")
        if (applicationId == "" || name == "") return null
        val apparentPlatform = view.optString("apparentPlatform", "")
        val hasNx = view.optBoolean("hasNxApplication", false) ||
                view.optBoolean("hasNxAddOnContents", false)
        val hasOunce = view.optBoolean("hasOunceApplication", false) ||
                view.optBoolean("hasOunceAddOnContents", false)
        return VgcTitle(
            applicationId = applicationId,
            name = name,
            isSwitch = apparentPlatform == "NX" || hasNx,
            isSwitch2 = apparentPlatform == "OUNCE" || hasOunce,
            hasApplication = view.optBoolean("hasApplication", false),
            hasAddOnContents = view.optBoolean("hasAddOnContents", false)
        )
    }

    // Add-on-only entries (DLC without a base game) are skipped on import,
    // matching Playnite's plugin
    fun isAddOnOnly(title: VgcTitle): Boolean {
        return !title.hasApplication && title.hasAddOnContents
    }

    /**
     * The platform set for a virtual game card: "Switch" and/or "Switch 2"
     * based on the card's platform flags, defaulting to "Switch".
     */
    fun platformsOf(title: VgcTitle): Set<String> {
        val platforms = mutableSetOf<String>()
        if (title.isSwitch) platforms.add("Switch")
        if (title.isSwitch2) platforms.add("Switch 2")
        if (platforms.isEmpty()) platforms.add("Switch")
        return platforms
    }

    // Common eShop name suffixes that don't appear in IGDB's canonical game
    // names. Matched case-insensitively at the end of a name.
    private val switchSuffixes = listOf(
        Regex("""\s*\(?for Nintendo Switch 2\)?\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(?for Nintendo Switch\)?\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[:\-–—]?\s*\(?\s*Nintendo Switch 2 Edition\s*\)?\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[:\-–—]?\s*\(?\s*Nintendo Switch Edition\s*\)?\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[:\-–—]?\s*\(?\s*Nintendo Switch 2 Version\s*\)?\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[:\-–—]?\s*\(?\s*Nintendo Switch Version\s*\)?\s*$""", RegexOption.IGNORE_CASE)
    )

    /**
     * Cleans a game name for IGDB matching: removes trademark symbols,
     * eShop "full game" suffixes, and Switch/Switch 2 edition suffixes
     * (like "for Nintendo Switch" or ": Nintendo Switch Edition"), and
     * collapses whitespace.
     */
    fun cleanTitle(name: String): String {
        var result = name
            .replace("™", " ")
            .replace("®", " ")
            .replace("©", " ")
            .replace("(full game)", " ", ignoreCase = true)
            .replace("full game", " ", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()
        // Strip edition suffixes repeatedly until the name is stable
        var previous: String
        do {
            previous = result
            for (suffix in switchSuffixes) {
                result = suffix.replace(result, "").trim()
            }
        } while (result != previous)
        return result
    }
}
