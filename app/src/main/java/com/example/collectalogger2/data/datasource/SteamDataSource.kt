package com.example.collectalogger2.data.datasource

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.SteamSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import org.json.JSONObject


/**
 * The bare minimum, you must get the steam library working.
 *
 */
class SteamDataSource(var userIdFlow: Flow<String>, gameDao: GameDao) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = "Steam"


    override suspend fun getGames(forceUpdate: Boolean): Flow<Game> = flow {
        // get the user ID
        val userId = userIdFlow.first()
        if (userId == "") throw AccountException("User is not logged into Steam!", libraryName)
        // so what you do, is that you gotta use the API-key
        // Make an API call to Steam
        val jsonObject = SteamSource.makeAPICall(
            endpoint = "IPlayerService",
            endpoint2 = "GetOwnedGames",
            version = 1,
            mapOf("steamid" to userId)
        )
        // assert that it has a list of games
        Log.i("Steam games fetched!", "")
        val responseJSON: JSONObject = jsonObject.get("response") as JSONObject
        val games = responseJSON.get("games") as JSONArray
        // this has many games, with URL as key and playtime as long
        val steamIdMap = mutableMapOf<String, Pair<Long, String>>()
        for (i in 0 until games.length()) {
            val apiGame = games.getJSONObject(i)

            var steamAppID: Long = (apiGame.get("appid") as Integer).toLong()
            // Quick check: If the Steam version of the game exists in the database already,
            // skip.
            var steamGame = gameDao.getGameBySteamId(steamAppID)
            var newPlayTime = (apiGame.get("playtime_forever") as Integer).toLong()
            if (!forceUpdate && steamGame != null) {
                var modifiedSteamGame = steamGame.copy(
                    platform = steamGame.platform.plus("PC"),
                    playTime = maxOf(steamGame.playTime, newPlayTime)
                )

                if (modifiedSteamGame != steamGame)
                    emit(modifiedSteamGame)
                continue
            }
            steamIdMap.put("https://store.steampowered.com/app/$steamAppID", newPlayTime to "$steamAppID")
        }
        callIGDB(
            gameIdentifiers = steamIdMap,
            identifierName = "url",
            endpoint = "websites",
            gamePrefix = "game.",
            customField = "url",
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            emit(game)
        }


    }

    override fun addToSourceLibrary(
        game: Game,
        localId: String
    ): Game {
        return game.copy(steamId = localId.toLong())
    }
}

