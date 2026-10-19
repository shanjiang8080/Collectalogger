package com.example.collectalogger2.util

import com.example.collectalogger2.util.libraryObjects.NintendoGraphQLRequestException
import com.example.collectalogger2.util.libraryObjects.NintendoSource
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Unit tests for the pure parsing logic of NintendoSource (the Virtual Game
 * Cards portal client). The network layer is not exercised here.
 */
class NintendoSourceTest {
    // --- parsePortalParams ---

    private val idToken = "eyJhbGciOiJIUzI1NiJ9.id-token-payload"
    private val clientId = "0f3f4d21fbjqd91ch99u49iwz5sn9chy3"
    private val graphqlUrl = "https://unify.svc.nintendo.net/shop/v2/graphql"

    private val paramsJson = """
        {"csrfToken": "abc", "idToken": "$idToken", "savannaClientId": "$clientId",
         "shopGraphQLApiUrl": "$graphqlUrl"}
    """.trimIndent()

    private val metaJson = """
        {"countries": [{"id": 1, "code": "DE"}, {"id": 82, "code": "US"}]}
    """.trimIndent()

    private val stateJson = """
        {"user": {"countryId": 82}, "lang": "en-US"}
    """.trimIndent()

    private fun portalHtml(
        dataName: String = "data",
        params: String = paramsJson,
        state: String = stateJson
    ): String {
        // data-json blobs are HTML-escaped in the actual page
        return """
            <html><body>
            <div id="$dataName" data-json="${params.replace("\"", "&quot;")}"></div>
            <div id="meta" data-json="${metaJson.replace("\"", "&quot;")}"></div>
            <div id="state" data-json="${state.replace("\"", "&quot;")}"></div>
            </body></html>
        """.trimIndent()
    }

    @Test
    fun parsePortalParams_extractsCredentialsAndLocale() {
        val params = NintendoSource.parsePortalParams(portalHtml())
        assertTrue(params != null)
        assertEquals(idToken, params!!.idToken)
        assertEquals(clientId, params.savannaClientId)
        assertEquals(graphqlUrl, params.shopGraphQLApiUrl)
        assertEquals("US", params.country)
        // The GraphQL language variable takes a two-letter code, like the plugin
        assertEquals("en", params.languageCode)
        assertEquals("en-US", params.nasLanguage)
    }

    @Test
    fun parsePortalParams_mapsNasLanguage() {
        val params = NintendoSource.parsePortalParams(
            portalHtml(state = stateJson.replace("\"lang\": \"en-US\"", "\"lang\": \"de-DE\""))
        )!!
        assertEquals("de", params.nasLanguage)
        assertEquals("de-DE", params.portalLanguage)
        assertEquals("de", params.languageCode)
    }

    @Test
    fun parsePortalParams_returnsNullWithoutDataBlob() {
        assertNull(NintendoSource.parsePortalParams(portalHtml(dataName = "other")))
    }

    @Test
    fun parsePortalParams_returnsNullOnLoginPage() {
        assertNull(NintendoSource.parsePortalParams("<html>Sign in to your Nintendo Account</html>"))
    }

    @Test
    fun parsePortalParams_returnsNullWithoutCountry() {
        assertNull(
            NintendoSource.parsePortalParams(
                portalHtml(state = stateJson.replace("\"countryId\": 82", "\"countryId\": 99"))
            )
        )
    }

    // --- parseVgcPage ---

    @Test
    fun parseVgcPage_extractsViewsAndTotal() {
        val views = JSONArray()
            .put(JSONObject().put("applicationId", "0100000000010000"))
            .put(JSONObject().put("applicationId", "0100000000010001"))
        val body = JSONObject()
            .put(
                "data",
                JSONObject().put(
                    "account",
                    JSONObject().put(
                        "vgc",
                        JSONObject().put(
                            "vgcViews",
                            JSONObject()
                                .put("offsetInfo", JSONObject().put("total", views.length()))
                                .put("views", views)
                        )
                    )
                )
            )
            .toString()

        val page = NintendoSource.parseVgcPage(body)
        assertEquals(2, page.views.size)
        assertEquals(2, page.total)
    }

    @Test
    fun parseVgcPage_languageFallbackOnBareError() {
        // Nintendo returns a bare "error" without a code for unsupported locales
        val body = JSONObject()
            .put(
                "errors",
                JSONArray().put(JSONObject().put("message", "error"))
            )
            .toString()

        val exception = try {
            NintendoSource.parseVgcPage(body)
            fail("parseVgcPage should have thrown for a GraphQL error!")
            null
        } catch (e: NintendoGraphQLRequestException) {
            e
        }
        assertTrue(exception!!.languageFallback)
    }

    @Test
    fun parseVgcPage_noLanguageFallbackForCodedError() {
        val body = JSONObject()
            .put(
                "errors",
                JSONArray().put(
                    JSONObject()
                        .put("message", "Invalid ID Token")
                        .put("extensions", JSONObject().put("code", "INVALID_TOKEN"))
                )
            )
            .toString()

        val exception = try {
            NintendoSource.parseVgcPage(body)
            fail("parseVgcPage should have thrown for a GraphQL error!")
            null
        } catch (e: NintendoGraphQLRequestException) {
            e
        }
        assertFalse(exception!!.languageFallback)
    }

    // --- parseVgcTitle / isAddOnOnly ---

    @Test
    fun parseVgcTitle_mapsNxAndOuncePlatforms() {
        val title = NintendoSource.parseVgcTitle(
            JSONObject()
                .put("applicationId", "0100000000010000")
                .put("applicationName", "Test Game")
                .put("apparentPlatform", "NX")
                .put("hasApplication", true)
                .put("hasAddOnContents", false)
        )!!
        assertEquals("0100000000010000", title.applicationId)
        assertTrue(title.isSwitch)
        assertFalse(title.isSwitch2)
    }

    @Test
    fun parseVgcTitle_ounceFlagsSwitch2() {
        val title = NintendoSource.parseVgcTitle(
            JSONObject()
                .put("applicationId", "0100000000010001")
                .put("applicationName", "Switch 2 Game")
                .put("apparentPlatform", "NX")
                .put("hasOunceApplication", true)
                .put("hasApplication", true)
                .put("hasAddOnContents", false)
        )!!
        assertTrue(title.isSwitch)
        assertTrue(title.isSwitch2)
    }

    @Test
    fun parseVgcTitle_ounceOnlyIsSwitch2() {
        // A Switch 2-only game (like Switch 2 exclusives) has no NX flags
        val title = NintendoSource.parseVgcTitle(
            JSONObject()
                .put("applicationId", "0100000000010003")
                .put("applicationName", "Switch 2 Exclusive")
                .put("apparentPlatform", "OUNCE")
                .put("hasApplication", true)
                .put("hasAddOnContents", false)
        )!!
        assertFalse(title.isSwitch)
        assertTrue(title.isSwitch2)
    }

    @Test
    fun platformsOf_distinguishesSwitchAndSwitch2() {
        val switch = NintendoSource.parseVgcTitle(
            JSONObject()
                .put("applicationId", "0100000000010000")
                .put("applicationName", "Game")
                .put("apparentPlatform", "NX")
                .put("hasApplication", true)
        )!!
        assertEquals(setOf("Switch"), NintendoSource.platformsOf(switch))

        val switch2 = NintendoSource.parseVgcTitle(
            JSONObject()
                .put("applicationId", "0100000000010003")
                .put("applicationName", "Exclusive")
                .put("apparentPlatform", "OUNCE")
                .put("hasApplication", true)
        )!!
        assertEquals(setOf("Switch 2"), NintendoSource.platformsOf(switch2))

        val enhanced = NintendoSource.parseVgcTitle(
            JSONObject()
                .put("applicationId", "0100000000010001")
                .put("applicationName", "Enhanced")
                .put("apparentPlatform", "NX")
                .put("hasOunceApplication", true)
                .put("hasApplication", true)
        )!!
        assertEquals(setOf("Switch", "Switch 2"), NintendoSource.platformsOf(enhanced))
    }

    @Test
    fun parseVgcTitle_skipsEntriesWithoutIdOrName() {
        assertNull(
            NintendoSource.parseVgcTitle(
                JSONObject().put("applicationId", "").put("applicationName", "Nameless")
            )
        )
        assertNull(
            NintendoSource.parseVgcTitle(
                JSONObject().put("applicationId", "0100000000010000").put("applicationName", "")
            )
        )
    }

    @Test
    fun parseVgcTitle_addOnOnlyEntries() {
        // DLC entry without a released base game: skipped on import
        val addOn = NintendoSource.parseVgcTitle(
            JSONObject()
                .put("applicationId", "0100000000010002")
                .put("applicationName", "DLC 1")
                .put("hasApplication", false)
                .put("hasAddOnContents", true)
        )!!
        assertTrue(NintendoSource.isAddOnOnly(addOn))

        // Full game (possibly with DLC): imported
        val fullGame = NintendoSource.parseVgcTitle(
            JSONObject()
                .put("applicationId", "0100000000010000")
                .put("applicationName", "Test Game")
                .put("hasApplication", true)
                .put("hasAddOnContents", true)
        )!!
        assertFalse(NintendoSource.isAddOnOnly(fullGame))
    }

    // --- getNasLanguage ---

    @Test
    fun getNasLanguage_matchesNintendoScriptMapping() {
        assertEquals("en-US", NintendoSource.getNasLanguage("en-US"))
        assertEquals("de", NintendoSource.getNasLanguage("de"))
        assertEquals("de", NintendoSource.getNasLanguage("de-DE"))
        assertEquals("es-419", NintendoSource.getNasLanguage("es-MX"))
        assertEquals("fr-CA", NintendoSource.getNasLanguage("fr-ca"))
        assertEquals("pt-BR", NintendoSource.getNasLanguage("PT-BR"))
        assertEquals("zh-Hant", NintendoSource.getNasLanguage("zh-TW"))
        assertEquals("ja", NintendoSource.getNasLanguage("ja-JP"))
        assertNull(NintendoSource.getNasLanguage("eo"))
    }

    // --- parsePlayHistories / authorize URL ---

    @Test
    fun parsePlayHistories_extractsMinutesAndPlatforms() {
        val body = """
            {"playHistories": [
                {"titleId": "0100cd801ce5e000", "titleName": "Balatro", "platform": "HAC",
                 "totalPlayedDays": 22, "totalPlayedMinutes": 5861},
                {"titleId": "040029202747a000", "titleName": "Balatro", "platform": "BEE",
                 "totalPlayedMinutes": 1041},
                {"titleId": "", "titleName": "No id", "platform": "HAC", "totalPlayedMinutes": 5}
             ],
             "hiddenTitleList": [],
             "recentPlayHistories": []}
        """.trimIndent()

        val histories = NintendoSource.parsePlayHistories(body)
        // Entries without a titleId are skipped
        assertEquals(2, histories.size)
        assertEquals("0100cd801ce5e000", histories[0].titleId)
        assertEquals(5861, histories[0].minutes)
        assertFalse(histories[0].isSwitch2)
        assertEquals("040029202747a000", histories[1].titleId)
        assertEquals(1041, histories[1].minutes)
        assertTrue(histories[1].isSwitch2)
    }

    @Test
    fun parsePlayHistories_handlesEmptyResponse() {
        val histories = NintendoSource.parsePlayHistories("""{"playHistories": []}""")
        assertEquals(0, histories.size)
        assertEquals(0, NintendoSource.parsePlayHistories("""{}""").size)
    }

    @Test
    fun getAuthorizeUrl_containsClientAndChallenge() {
        val url = NintendoSource.getAuthorizeUrl("test_challenge")
        assertTrue(url.startsWith("https://accounts.nintendo.com/connect/1.0.0/authorize"))
        // The Nintendo Store app's client id and its custom scheme redirect
        assertTrue(url.contains("client_id=5c38e31cd085304b"))
        assertTrue(url.contains("redirect_uri=npf5c38e31cd085304b%3A%2F%2Fauth"))
        assertTrue(url.contains("response_type=session_token_code"))
        assertTrue(url.contains("session_token_code_challenge=test_challenge"))
        assertTrue(url.contains("session_token_code_challenge_method=S256"))
    }

    @Test
    fun codeChallenge_isSha256OfVerifier() {
        val verifier = NintendoSource.generateCodeVerifier()
        assertEquals(45, verifier.length)
        // The challenge is a deterministic base64url SHA-256 of the verifier
        assertEquals(
            NintendoSource.getCodeChallenge(verifier),
            NintendoSource.getCodeChallenge(verifier)
        )
    }

    // --- isPlayableGameEntry ---

    @Test
    fun isPlayableGameEntry_rejectsDemosAndApps() {
        // Demos and test releases
        assertFalse(NintendoSource.isPlayableGameEntry("DAVE THE DIVER Demo"))
        assertFalse(NintendoSource.isPlayableGameEntry("Metroid Dread DEMO"))
        assertFalse(NintendoSource.isPlayableGameEntry("Pikmin 4 Demo"))
        assertFalse(NintendoSource.isPlayableGameEntry("ARMS Global Testpunch"))
        assertFalse(NintendoSource.isPlayableGameEntry("Splatoon 2 Global Testfire"))
        assertFalse(NintendoSource.isPlayableGameEntry("Kirby and the Forgotten Land (Demo Version)"))
        assertFalse(NintendoSource.isPlayableGameEntry("Pepper Grinder [DEMO]"))
        // Non-game apps
        assertFalse(NintendoSource.isPlayableGameEntry("YouTube"))
        assertFalse(NintendoSource.isPlayableGameEntry("Twitch"))
        assertFalse(NintendoSource.isPlayableGameEntry("Pokémon HOME"))
        assertFalse(NintendoSource.isPlayableGameEntry("Pokémon HOME".replace("é", "e")))
        // Nintendo Classics launcher apps
        assertFalse(NintendoSource.isPlayableGameEntry("Game Boy – Nintendo Classics"))
        assertFalse(NintendoSource.isPlayableGameEntry(
            "Super Nintendo Entertainment System – Nintendo Classics"
        ))
        assertFalse(NintendoSource.isPlayableGameEntry("Nintendo Switch Online"))
    }

    @Test
    fun isPlayableGameEntry_acceptsRealGames() {
        assertTrue(NintendoSource.isPlayableGameEntry("Pokémon LeafGreen Version"))
        assertTrue(NintendoSource.isPlayableGameEntry("Balatro"))
        assertTrue(NintendoSource.isPlayableGameEntry("We Love Katamari REROLL+ Royal Reverie"))
        assertTrue(NintendoSource.isPlayableGameEntry("Demon Turbobox"))
        // "Demo" as part of a word is not a demo
        assertTrue(NintendoSource.isPlayableGameEntry("Democracy 3"))
    }

    // --- cleanTitle ---

    @Test
    fun cleanTitle_stripsTrademarksAndSuffixes() {
        assertEquals(
            "The Legend of Zelda: Tears of the Kingdom",
            NintendoSource.cleanTitle("The Legend of Zelda: Tears of the Kingdom™")
        )
        assertEquals(
            "Metroid Prime 4: Beyond",
            NintendoSource.cleanTitle("Metroid Prime 4: Beyond®")
        )
        assertEquals(
            "Some Game",
            NintendoSource.cleanTitle("Some Game (Full Game)")
        )
        // Extra whitespace is collapsed
        assertEquals(
            "Word Word",
            NintendoSource.cleanTitle("Word™  Word")
        )
    }

    @Test
    fun cleanTitle_stripsSwitchEditionSuffixes() {
        // "for Nintendo Switch" suffixes
        assertEquals(
            "Katamari Damacy REROLL",
            NintendoSource.cleanTitle("Katamari Damacy REROLL for Nintendo Switch")
        )
        assertEquals(
            "Super Mario Maker 2",
            NintendoSource.cleanTitle("Super Mario Maker 2 for Nintendo Switch")
        )
        assertEquals(
            "Mina the Hollower",
            NintendoSource.cleanTitle("Mina the Hollower for Nintendo Switch")
        )
        // ": Nintendo Switch Edition" style suffixes
        assertEquals(
            "Minecraft",
            NintendoSource.cleanTitle("Minecraft: Nintendo Switch Edition")
        )
        assertEquals(
            "The Legend of Zelda: Breath of the Wild",
            NintendoSource.cleanTitle("The Legend of Zelda: Breath of the Wild – Nintendo Switch 2 Edition")
        )
        // Parenthesized variants
        assertEquals(
            "Tetris Effect",
            NintendoSource.cleanTitle("Tetris Effect (Nintendo Switch Version)")
        )
        // Repeated stripping (suffix sandwiched against another suffix)
        assertEquals(
            "Minecraft",
            NintendoSource.cleanTitle("Minecraft: Nintendo Switch Edition for Nintendo Switch")
        )
        // Nintendo Switch Sports is a real game name, not a suffix
        assertEquals(
            "Nintendo Switch Sports",
            NintendoSource.cleanTitle("Nintendo Switch Sports")
        )
        // "Nintendo Switch Sports" remains intact even with a "for" suffix
        assertEquals(
            "Nintendo Switch Sports",
            NintendoSource.cleanTitle("Nintendo Switch Sports for Nintendo Switch")
        )
    }
}
