package com.example.collectalogger2.data.datasource

import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.GameEvent.ExpectedGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.FinishGamesCount
import com.example.collectalogger2.data.datasource.GameEvent.GameLoaded
import com.example.collectalogger2.data.datasource.GameEvent.ListNonImportedGames
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.libraryObjects.ItchSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

class ItchDataSource(var secretFlow: Flow<String>, gameDao: GameDao) :
    RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = name

    companion object : HasLibraryName {
        override val name = "Itch"
    }

    override suspend fun getGames(forceUpdate: Boolean): Flow<GameEvent> = flow {
        val secret = secretFlow.first()
        if (secret == "") throw AccountException("User is not logged into Itch!", libraryName)
        var page = 1
        var jsonObject: JSONObject
        // URL as key, playtime (0) and itchId as value
        var itchIdMap = mutableMapOf<String, Pair<Long, String>>()
        // A map resolving ids to game names
        var idNameMap = mutableMapOf<String, String>()
        do {
            jsonObject = ItchSource.makeAPICall(secret, page)
            if (jsonObject.get("owned_keys") is JSONObject) break
            val ownedKeys = jsonObject.getJSONArray("owned_keys")
            val pageLength = ownedKeys.length()
            page++
            for (i in 0 until pageLength) {
                val jsonItem = ownedKeys.getJSONObject(i)
                val jsonGame = jsonItem.getJSONObject("game")
                if (jsonGame.getString("classification") != "game") continue
                itchIdMap.put(jsonGame.getString("url"), 0L to "${jsonGame.getInt("id")}")
                idNameMap.put("${jsonGame.getInt("id")}", jsonGame.getString("title"))
            }
        } while (jsonObject.has("owned_keys") && jsonObject.getJSONArray("owned_keys")
                .length() != 0
        )

        // Emit an expected games count
        emit(ExpectedGamesCount(itchIdMap.size))

        callIGDB(
            gameIdentifiers = itchIdMap,
            identifierName = "url",
            endpoint = "websites",
            gamePrefix = "game.",
            customField = "url",
            includeUpdates = true,
            customFieldIsGameAttribute = false,
            otherGameFilter = "",
            gameJSONPath = { json -> return@callIGDB json.getJSONObject("game") }
        ).forEach { game ->
            emit(GameLoaded(game))
            idNameMap.remove(game.itchId)
        }

        emit(FinishGamesCount)

        // idNameMap contains leftover games
        // try to fallback before giving up
        var fallbackMap = mutableMapOf<String, Pair<Long, String>>()
        idNameMap.forEach { game ->
            fallbackMap.put(game.value, 0L to game.key)
        }
        callIGDB(
            gameIdentifiers = fallbackMap,
            identifierName = "name",
            endpoint = "games",
            gamePrefix = "",
            customField = "name",
            includeUpdates = true,
            otherGameFilter = "websites.type.type = \"Itch\"",
            gameJSONPath = { json -> return@callIGDB json }
        ).forEach { game ->
            emit(GameLoaded(game))
            idNameMap.remove(game.itchId)
        }


        val missingGames = mutableListOf<Game>()
        idNameMap.forEach { game ->
            missingGames.add(
                Game(
                    itchId = game.key,
                    title = game.value
                )
            )
        }

        emit(ListNonImportedGames(missingGames))


    }

    override fun copyWithID(
        game: Game,
        gameWithId: Game
    ): Game {
        return game.copy(itchId = gameWithId.itchId)
    }

    override fun addToSourceLibrary(
        game: Game,
        localId: String
    ): Game {
        return game.copy(itchId = localId)
    }


}