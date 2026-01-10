package com.example.collectalogger2.data.datasource

import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.GameEvent.ExpectedGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.FinishGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.GameLoaded
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.libraryObjects.GogSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class GogDataSource(var usernameFlow: Flow<String>, gameDao: GameDao) :
    RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = name

    companion object : HasLibraryName {
        override val name = "GOG"
    }

    override suspend fun getGames(forceUpdate: Boolean): Flow<GameEvent> = flow {
        val userId = usernameFlow.first()
        if (userId == "") throw AccountException("User is not logged into GOG!", libraryName)
        // URL as key, pair of playtime and localId as value
        val gogIdMap = mutableMapOf<String, Pair<Long, String>>()
        // Make the API call
        var page = 1
        var pages = 0
        var countEmitted = false
        do {
            val jsonObject = GogSource.makeAPICall(
                username = userId,
                page = page
            )
            if (!countEmitted) {
                emit(ExpectedGamesCount(jsonObject.getInt("total")))
                countEmitted = true
            }
            pages = jsonObject.getInt("pages")
            var items = jsonObject.getJSONObject("_embedded").getJSONArray("items")
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val game = item.getJSONObject("game")
                val stats = item.get("stats")
                var playTime = 0L
                if (stats is JSONObject) {
                    stats.keys().forEach { key ->
                        playTime = stats.getJSONObject(key).getLong("playtime")
                    }
                }
                val id = game.getString("id")
                val existingGame = gameDao.getGameByGogId(id)
                if (!forceUpdate && existingGame != null) {
                    var modifiedGame = existingGame.copy(
                        platform = existingGame.platform.plus("PC"),
                        playTime = maxOf(existingGame.playTime, playTime)
                    )
                    if (modifiedGame != existingGame)
                        emit(GameLoaded(modifiedGame))
                    continue
                }
                // Note: remove the /en/ from the thing
                // or regular processing
                val url = game.getString("url")
                val urlSegmentNoLanguage = url.substring(url.indexOf("/", startIndex = 1))
                gogIdMap["https://www.gog.com$urlSegmentNoLanguage"] =
                    playTime to id
            }
            page++

        } while (page <= pages)

        callIGDB(
            gameIdentifiers = gogIdMap,
            identifierName = "url", // ?
            endpoint = "websites",
            gamePrefix = "game.",
            customField = "url", // ?
            includeUpdates = true,
            customFieldIsGameAttribute = false, // ?
            otherGameFilter = "", // ?
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            emit(GameLoaded(game))
        }

        emit(FinishGamesCount)

        // TODO emit non-imported games
    }

    override fun copyWithID(
        game: Game,
        gameWithId: Game
    ): Game {
        return game.copy(gogId = gameWithId.gogId)
    }

    override fun addToSourceLibrary(
        game: Game,
        localId: String
    ): Game {
        return game.copy(gogId = localId)
    }

}