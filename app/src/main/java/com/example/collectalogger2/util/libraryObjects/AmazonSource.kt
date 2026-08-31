package com.example.collectalogger2.util.libraryObjects

import android.os.Build
import android.util.Log
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.APIStatusException
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * API client for Amazon Games (the Prime Gaming / AGSLauncher backend).
 * Reverse-engineered from Playnite's AmazonGamesLibrary extension:
 * https://github.com/JosefNemec/PlayniteExtensions/tree/master/source/Libraries/AmazonGamesLibrary
 *
 * The flow is:
 * 1. The user logs into Amazon in a WebView with a PKCE code challenge
 *    (LOGIN_URL + code challenge). Amazon redirects to a URL containing the
 *    "openid.oa2.authorization_code" query parameter.
 * 2. The authorization code + code verifier are exchanged for device tokens
 *    by registering the device with api.amazon.com (registerDevice).
 * 3. The refresh token is exchanged for fresh access tokens whenever needed
 *    (refreshTokens); access tokens expire after roughly an hour.
 * 4. Owned games are listed by paging the Animus entitlements service
 *    (getEntitlementsPage).
 */
object AmazonSource {
    private const val TAG = "AmazonSource"

    // The client id and device type that the Amazon Games launcher uses.
    private const val CLIENT_ID =
        "3733646238643238366332613932346432653737653161663637373636363435234132554d56484f58375550345637"
    private const val DEVICE_TYPE = "A2UMVHOX7UP4V7"

    /**
     * The Amazon sign-in page. Once the user signs in, Amazon redirects them to
     * a URL containing the "openid.oa2.authorization_code" query parameter.
     * The code challenge must be appended to the end.
     */
    const val LOGIN_URL =
        "https://www.amazon.com/ap/signin?openid.ns=http://specs.openid.net/auth/2.0" +
        "&openid.claimed_id=http://specs.openid.net/auth/2.0/identifier_select" +
        "&openid.identity=http://specs.openid.net/auth/2.0/identifier_select" +
        "&openid.mode=checkid_setup" +
        "&openid.oa2.scope=device_auth_access" +
        "&openid.ns.oa2=http://www.amazon.com/ap/ext/oauth/2" +
        "&openid.oa2.response_type=code" +
        "&openid.oa2.code_challenge_method=S256" +
        "&openid.oa2.client_id=device:$CLIENT_ID" +
        "&language=en_US&marketPlaceId=ATVPDKIKX0DER" +
        "&openid.return_to=https://www.amazon.com" +
        "&openid.pape.max_auth_age=0" +
        "&openid.assoc_handle=amzn_sonic_games_launcher" +
        "&pageId=amzn_sonic_games_launcher" +
        "&openid.oa2.code_challenge="

    // Not sure what key this is but it's some key from Amazon.Fuel.Plugin.Entitlement.dll
    // (same comment as Playnite's)
    private const val ENTITLEMENTS_KEY_ID = "d5dc8b8b-86c8-4fc4-ae93-18c0def5314d"

    private val client = HttpClient(Android)
    private val mutex = Mutex()
    private var lastRequestTime = 0L
    private const val RATE_LIMIT_DELAY_MS = 250L

    /**
     * Generates a PKCE code verifier, a random string of 45 chars
     * (like Playnite's GenerateCodeChallenge).
     */
    fun generateCodeVerifier(): String {
        val randomStringChars = "ABCDEFGHIJKLMNOPQRSTYVWXZabcdefghijklmnopqrstyvwxz0123456789_"
        val result = StringBuilder(45)
        repeat(45) {
            result.append(randomStringChars[Random.nextInt(0, randomStringChars.length - 1)])
        }
        return result.toString()
    }

    /**
     * Generates the S256 code challenge from the code verifier:
     * the SHA-256 hash of the verifier, base64url-encoded without padding.
     */
    fun getCodeChallenge(codeVerifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray(Charsets.UTF_8))
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * Exchanges the authorization code from the login redirect (plus the code
     * verifier used for the challenge) for device bearer tokens. Returns a
     * JSONObject with "access_token" and "refresh_token".
     */
    suspend fun registerDevice(authorizationCode: String, codeVerifier: String): JSONObject {
        val reqData = JSONObject()
        val authData = JSONObject()
        authData.put("use_global_authentication", false)
        authData.put("authorization_code", authorizationCode)
        authData.put("code_verifier", codeVerifier)
        authData.put("code_algorithm", "SHA-256")
        authData.put("client_id", CLIENT_ID)
        authData.put("client_domain", "DeviceLegacy")
        reqData.put("auth_data", authData)

        val registrationData = JSONObject()
        registrationData.put("app_name", "AGSLauncher for Windows")
        registrationData.put("app_version", "1.0.0")
        registrationData.put("device_model", "Windows")
        registrationData.put("device_serial", UUID.randomUUID().toString().replace("-", ""))
        registrationData.put("device_type", DEVICE_TYPE)
        registrationData.put("domain", "Device")
        registrationData.put("os_version", "${Build.VERSION.RELEASE}")
        reqData.put("registration_data", registrationData)

        reqData.put("requested_extensions", org.json.JSONArray(listOf("customer_info", "device_info")))
        reqData.put("requested_token_type", org.json.JSONArray(listOf("bearer", "mac_dms")))
        reqData.put("user_context_map", JSONObject())

        val response = postRequest(
            url = "https://api.amazon.com/auth/register",
            customHeaders = mapOf("User-Agent" to "AGSLauncher/1.0.0"),
            body = reqData.toString()
        )
        val authDataResponse = JSONObject(response)
        if (!authDataResponse.has("response") ||
            !authDataResponse.getJSONObject("response").has("success")
        ) {
            throw APIException("Amazon device registration failed with response: $response")
        }
        return authDataResponse
            .getJSONObject("response")
            .getJSONObject("success")
            .getJSONObject("tokens")
            .getJSONObject("bearer")
    }

    /**
     * Exchanges the refresh token for a fresh access token. Returns the
     * response JSONObject, which contains "access_token".
     */
    suspend fun refreshTokens(refreshToken: String): JSONObject {
        val reqData = JSONObject()
        reqData.put("app_name", "AGSLauncher")
        reqData.put("app_version", "3.0.9495.3")
        reqData.put("source_token", refreshToken)
        reqData.put("requested_token_type", "access_token")
        reqData.put("source_token_type", "refresh_token")

        val response = postRequest(
            url = "https://api.amazon.com/auth/token",
            customHeaders = mapOf("User-Agent" to "AGSLauncher/1.0.0"),
            body = reqData.toString()
        )
        val tokenResponse = JSONObject(response)
        if (!tokenResponse.has("access_token")) {
            throw APIException("Amazon token refresh failed with response: $response")
        }
        return tokenResponse
    }

    /**
     * Gets a page of the account's game entitlements. Returns the response
     * JSONObject with "entitlements" (JSONArray) and possibly "nextToken"
     * for pagination.
     */
    suspend fun getEntitlementsPage(accessToken: String, nextToken: String?): JSONObject {
        val reqData = JSONObject()
        reqData.put("Operation", "GetEntitlements")
        reqData.put("clientId", "Sonic")
        reqData.put("syncPoint", 0)
        reqData.put("nextToken", nextToken ?: JSONObject.NULL)
        reqData.put("maxResults", 500)
        reqData.put("keyId", ENTITLEMENTS_KEY_ID)
        reqData.put("hardwareHash", UUID.randomUUID().toString().replace("-", ""))
        reqData.put("productIdFilter", JSONObject.NULL)
        reqData.put("disableStateFilter", true)

        val response = postRequest(
            url = "https://gaming.amazon.com/api/distribution/entitlements",
            customHeaders = mapOf(
                "User-Agent" to "com.amazon.agslauncher.win/3.0.9495.3",
                "X-Amz-Target" to
                    "com.amazon.animusdistributionservice.entitlement.AnimusEntitlementsService.GetEntitlements",
                "x-amzn-token" to accessToken,
                "Content-Encoding" to "amz-1.0"
            ),
            body = reqData.toString()
        )
        return JSONObject(response)
    }

    /**
     * Makes a POST request with a JSON body and returns the response text.
     * Throws an APIStatusException on non-successful HTTP statuses.
     */
    private suspend fun postRequest(url: String, customHeaders: Map<String, String>, body: String): String {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < RATE_LIMIT_DELAY_MS) {
                delay((RATE_LIMIT_DELAY_MS - elapsed).milliseconds)
            }
            lastRequestTime = System.currentTimeMillis()

            try {
                val response = client.post(url) {
                    headers { customHeaders.forEach { header -> append(header.key, header.value) } }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
                if (!response.status.isSuccess()) {
                    val errorText = response.bodyAsText()
                    throw APIStatusException(
                        "Amazon API call to $url failed with HTTP status ${response.status.value}: $errorText",
                        response.status.value
                    )
                }
                return response.bodyAsText()
            } catch (ex: APIException) {
                throw ex
            } catch (ex: Exception) {
                Log.e(TAG, "Amazon API call to $url failed!", ex)
                throw APIException("Amazon API call failed with message: ${ex.message}")
            }
        }
    }
}
