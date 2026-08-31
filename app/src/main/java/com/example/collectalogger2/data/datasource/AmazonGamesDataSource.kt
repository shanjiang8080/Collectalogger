package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.GameEvent.ExpectedGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.FinishGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.GameLoaded
import com.example.collectalogger2.data.datasource.GameEvent.ListNonImportedGames
import com.example.collectalogger2.util.APIStatusException
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.AccountExpiryException
import com.example.collectalogger2.util.libraryObjects.AmazonSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Imports the user's Amazon Games library (Prime Gaming).
 * Based on the flow of Playnite's AmazonGamesLibrary extension:
 * fetch the account's entitlements, then match them to IGDB games.
 * Amazon doesn't provide playtime, so playTime is always 0.
 */
class AmazonGamesDataSource(
    var userInfoFlow: Flow<String>,
    var userInfoSetter: suspend (String) -> Unit,
    gameDao: GameDao
) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = name

    companion object : HasLibraryName {
        override val name = "Amazon"
    }

    override suspend fun getGames(forceUpdate: Boolean): Flow<GameEvent> = flow {
        val userInfo = userInfoFlow.first()
        if (userInfo == "") throw AccountException("User is not logged into Amazon Games!", name)
        // Refresh the access token, since it expires after about an hour
        val userJson = refreshLogin(JSONObject(userInfo))
        val accessToken = userJson.getString("access_token")

        // Map of ASIN to (playtime, amazonId), used to match games with IGDB
        var asinMap = mutableMapOf<String, Pair<Long, String>>()
        // Map of amazonId to title, for the name-based fallback and non-imported games
        val idTitleMap = mutableMapOf<String, String>()

        // Get all the entitlements, paging through the response
        var nextToken: String? = null
        do {
            val response = AmazonSource.getEntitlementsPage(accessToken, nextToken)
            val entitlements = response.optJSONArray("entitlements") ?: JSONArray()
            for (i in 0 until entitlements.length()) {
                val entitlement = entitlements.getJSONObject(i)
                val product = entitlement.optJSONObject("product") ?: continue
                // Twitch channel games are not part of the Amazon Games library
                if (product.optString("productLine") == "Twitch:FuelEntitlement") continue
                val amazonId = product.optString("id")
                val title = product.optString("title")
                if (amazonId == "" || title == "") continue
                val asin = product.optString("asin")
                if (asin != "" && asin !in asinMap) {
                    asinMap[asin] = 0L to amazonId
                }
                idTitleMap.putIfAbsent(amazonId, title)
            }
            nextToken = if (response.has("nextToken") && !response.isNull("nextToken")) {
                response.getString("nextToken")
            } else {
                null
            }
        } while (nextToken != null)

        // Emit an expected games count for the loading bar
        emit(ExpectedGamesCount(idTitleMap.size))

        // Skip games that are already in the database (unless forcing an update)
        for ((amazonId, _) in idTitleMap.toList()) {
            val existingGame = gameDao.getGameByAmazonId(amazonId)
            if (!forceUpdate && existingGame != null) {
                val modifiedGame = existingGame.copy(
                    platform = existingGame.platform.plus("PC")
                )
                if (modifiedGame != existingGame)
                    emit(GameLoaded(modifiedGame))
                idTitleMap.remove(amazonId)
                Log.d("AmazonGamesDataSource", "$amazonId skipped since it exists in the database")
            }
        }
        // Also remove the ASINs that point to skipped games
        asinMap = asinMap.filter { it.value.second in idTitleMap }.toMutableMap()

        val duplicateSet = mutableSetOf<Long>() // Contains game ids for duplicate handling

        // Primary pass: match games by their ASIN through IGDB's external_games,
        // where amazon_asin (external_game_source 20) entries store the ASIN in uid.
        Log.d("AmazonGamesDataSource", "Making bulk IGDB call with ASINs")
        callIGDB(
            gameIdentifiers = asinMap,
            identifierName = "uid",
            endpoint = "external_games",
            gamePrefix = "game.",
            customField = "uid",
            customFieldIsGameAttribute = false,
            otherGameFilter = "external_game_source = 20",
            includeUpdates = true,
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            if (game.igdbId !in duplicateSet) {
                emit(GameLoaded(game))
                idTitleMap.remove(game.amazonId)
                duplicateSet.add(game.igdbId)
            }
        }

        // Fallback pass: match the remaining games by title
        val fallbackMap = mutableMapOf<String, Pair<Long, String>>()
        idTitleMap.forEach { (amazonId, title) ->
            fallbackMap[sanitizeTitle(title)] = 0L to amazonId
        }
        if (fallbackMap.isNotEmpty()) {
            Log.d("AmazonGamesDataSource", "Making bulk IGDB call for fallbacks with names")
            callIGDB(
                gameIdentifiers = fallbackMap,
                identifierName = "name",
                endpoint = "games",
                gamePrefix = "",
                customField = "name",
                customFieldIsGameAttribute = false,
                includeUpdates = true,
                gameJSONPath = { json -> return@callIGDB json }
            ).forEach { game ->
                if (game.igdbId !in duplicateSet) {
                    emit(GameLoaded(game))
                    idTitleMap.remove(game.amazonId)
                    duplicateSet.add(game.igdbId)
                }
            }
        }

        // Emit a FinishGamesCount
        emit(FinishGamesCount)

        // Emit non-imported games with their names and IDs
        val newMissingGames = mutableListOf<Game>()
        idTitleMap.forEach { (amazonId, title) ->
            newMissingGames.add(
                Game(
                    title = title,
                    amazonId = amazonId
                )
            )
        }
        if (newMissingGames.isNotEmpty()) {
            emit(ListNonImportedGames(newMissingGames))
        }
    }

    override fun copyWithID(
        game: Game,
        gameWithId: Game
    ): Game {
        return game.copy(amazonId = gameWithId.amazonId)
    }

    override fun addToSourceLibrary(
        game: Game,
        localId: String
    ): Game {
        if (localId == "") {
            Log.w("AmazonGamesDataSource", "localId is empty, game will have no Amazon id!")
            return game
        }
        return game.copy(amazonId = localId)
    }

    /**
     * Refreshes the stored tokens if needed (the access token expires after
     * about an hour, so always refresh it). Throws an AccountExpiryException
     * if the refresh token is no longer valid.
     */
    private suspend fun refreshLogin(json: JSONObject): JSONObject {
        try {
            val newToken = AmazonSource.refreshTokens(json.getString("refresh_token"))
            json.put("access_token", newToken.getString("access_token"))
            // Amazon only returns a new refresh token if the old one is close to expiry
            if (newToken.has("refresh_token") && !newToken.isNull("refresh_token")) {
                json.put("refresh_token", newToken.getString("refresh_token"))
            }
            userInfoSetter(json.toString())
            return json
        } catch (e: APIStatusException) {
            // A 400 means the refresh token is invalid or expired, so the user
            // has to log in again. Other statuses are rethrown to be retried.
            if (e.statusCode == 400) {
                throw AccountExpiryException(
                    "Cannot refresh login! Must log in again to Amazon Games!",
                    libraryName
                )
            }
            throw e
        }
    }
}

/**
 * Sanitizes a title for the IGDB name matching, removing trademark symbols
 * and escaping quotes.
 */
private fun sanitizeTitle(title: String): String {
    return title
        .replace("™", "")
        .replace("®", "")
        .replace("\"", "\\\"")
}
