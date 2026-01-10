package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.GameEvent.ExpectedGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.FinishGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.GameLoaded
import com.example.collectalogger2.data.datasource.GameEvent.ListNonImportedGames
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.libraryObjects.SteamSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.json.JSONObject


/**
 * The bare minimum, you must get the steam library working.
 *
 */
class SteamDataSource(var userIdFlow: Flow<String>, gameDao: GameDao) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = name

    companion object : HasLibraryName {
        override val name = "Steam"
    }

    override suspend fun getGames(forceUpdate: Boolean): Flow<GameEvent> = flow {
        // Get the user ID
        val userId = userIdFlow.first()
        if (userId == "") throw AccountException("User is not logged into Steam!", libraryName)
        // Make an API call to Steam
        val jsonObject = SteamSource.makeAPICall(
            endpoint = "IPlayerService",
            endpoint2 = "GetOwnedGames",
            version = 1,
            mapOf(
                "steamid" to userId,
                "include_played_free_games" to "true",
                "skip_unvetted_apps" to "false"
            )
        )
        Log.i("Steam games fetched!", "")
        val responseJSON: JSONObject = jsonObject.getJSONObject("response")
        val games = responseJSON.getJSONArray("games")
        // Emit a ExpectedGamesCount
        emit(ExpectedGamesCount(games.length()))
        // this has many games, with URL as key and playtime as long
        val steamIdMap = mutableMapOf<String, Pair<Long, String>>()
        for (i in 0 until games.length()) {
            val apiGame = games.getJSONObject(i)

            var steamAppID: Long = (apiGame.get("appid") as Integer).toLong()
            // Quick check: If the Steam version of the game exists in the database already, skip
            var steamGame = gameDao.getGameBySteamId(steamAppID)
            var newPlayTime = (apiGame.get("playtime_forever") as Integer).toLong()
            if (!forceUpdate && steamGame != null) {
                var modifiedSteamGame = steamGame.copy(
                    platform = steamGame.platform.plus("PC"),
                    playTime = maxOf(steamGame.playTime, newPlayTime)
                )

                if (modifiedSteamGame != steamGame)
                    emit(GameLoaded(modifiedSteamGame))
                continue
            }
            steamIdMap.put("$steamAppID", newPlayTime to "$steamAppID")
        }
        callIGDB(
            gameIdentifiers = steamIdMap,
            identifierName = "uid",
            endpoint = "external_games",
            gamePrefix = "game.",
            customField = "uid",
            includeUpdates = true,
            customFieldIsGameAttribute = false,
            otherGameFilter = "external_game_source.name = \"Steam\"",
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            emit(GameLoaded(game))
            // Remove it from the map
            steamIdMap.remove("${game.steamId}")
        }

        // Emit a finished games count
        emit(FinishGamesCount)

        // non-public games like metro last light complete edition can get skipped here
        val missingGames = mutableListOf<Game>()
        steamIdMap.forEach { game ->
            missingGames.add(
                // TODO there should be the game's title so it can be identified
                Game(
                    steamId = game.key.toLong(),
                    playTime = game.value.first
                )
            )
        }
        emit(ListNonImportedGames(missingGames))
    }

    override fun copyWithID(
        game: Game,
        gameWithId: Game
    ): Game {
        return game.copy(steamId = gameWithId.steamId)
    }

    override fun addToSourceLibrary(
        game: Game,
        localId: String
    ): Game {
        return game.copy(steamId = localId.toLong())
    }
}

