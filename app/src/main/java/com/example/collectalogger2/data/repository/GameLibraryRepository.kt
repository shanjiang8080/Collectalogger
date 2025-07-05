package com.example.collectalogger2.data.repository

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.LocalDataSource
import com.example.collectalogger2.data.datasource.RemoteLibraryDataSource
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.APIStatusException
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.IGDBSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import org.json.JSONArray
import org.json.JSONObject


// I should replace the `remoteLibraryDataSource` with a list of them
class GameLibraryRepository(
    private val remoteLibraryDataSources: List<RemoteLibraryDataSource>,
    private val localDataSource: LocalDataSource?,
    private val gameDao: GameDao
) {
    fun getAllGamesStream(): Flow<List<Game>> = gameDao.getAllGamesStream()

    fun getAllGames(): List<Game> = gameDao.getAllGames()

    fun getGameStream(id: Long): Flow<Game?> = gameDao.getGameStream(id)

    fun getGameById(id: Long): Game? = gameDao.getGame(id)

    fun getGameByIGDBId(IGDBId: Long): Game? = gameDao.getGameByIGDBId(IGDBId)

    fun getGameStreamByIGDBId(IGDBId: Long): Flow<Game?> = gameDao.getGameStreamByIGDBId(IGDBId)

    suspend fun insertGame(game: Game) = gameDao.insert(game)

    suspend fun deleteGame(game: Game) = gameDao.delete(game)

    suspend fun updateGame(game: Game) = gameDao.update(game)

    suspend fun updateGameLibraries() {
        for (dataSource in remoteLibraryDataSources) {
            var retriesLeft = 3
            while (retriesLeft > 0) {
                // This block handles errors but is coarse-grained.
                // TODO add fine-grained failures in the getGames method(s).
                try {
                    val games = dataSource.getGames()
                    games
                        .collect { game ->
                            // See if it's here, if there is one
                            val existingGame = gameDao.getGameByIGDBId(game.igdbId)
                            if (existingGame != null) {
                                /*
                                update the game with the new information
                                TODO edit this to append Source, Genre, Platform of games instead of replacing
                                */
                                val highestPlaytime = maxOf(existingGame.playTime, game.playTime)
                                val newGame = game.copy(id = existingGame.id, playTime = highestPlaytime)
                                updateGame(newGame)
                                Log.i("Game updated!", "{title: ${game.title}, igdbId: ${game.igdbId}, id: ${game.id}}")
                            } else {
                                // add the new game
                                insertGame(game)
                                Log.i("Game added!", "{title: ${game.title}, igdbId: ${game.igdbId}, id: ${game.id}}")
                            }
                        }
                }
                catch (e: Exception) {
                    if (e is APIStatusException) {
                        Log.e("APIStatusException happened with HTTP code ${e.statusCode}", e.message ?: "")
                    } else if (e is APIException) {
                        Log.e("APIException happened", e.message ?: "")
                    } else if (e is AccountException) {
                        Log.e("AccountException happened", e.message ?: "")
                        break // go to the next DataSource
                    }
                    else {
                        Log.e("Exception happened", e.message ?: "")
                        throw e
                    }
                    retriesLeft--
                    delay(5000) // Retry with delay
                    continue
                }
                break
            }
        }
    }

    /**
     * This method calls IGDB and gets extra information like the game background.
     * It returns the updated Game.
     * TODO will get more information in the future (like websites)
     */
    suspend fun getAuxiliaryInformation(game: Game): Game {
        var updatedGame = game.copy()
        var igdbResponse: JSONArray
        try {
            igdbResponse = IGDBSource.makeAPICall(
                "artworks",
                "fields image_id; where game = ${game.igdbId};"
            )
        } catch (ex: Exception) {
            if (ex is APIException) {
                Log.e("Connection to IGDB API Proxy failed.", ex.message ?: "")
            } else {
                Log.e("IGDB Source failed.", ex.message ?: "")
            }
            return game
        }
        if (igdbResponse.length() > 0 && (igdbResponse.get(0) as JSONObject).has("image_id")) {
            var igdbResponseObj = igdbResponse.get(0) as JSONObject
            var imageId = igdbResponseObj.get("image_id")
            updatedGame = updatedGame.copy(backgroundUrl = "https://images.igdb.com/igdb/image/upload/t_720p/${imageId}.jpg")
        } else {
            Log.w("This game has no artwork!", game.title)
        }

        // TODO other metadata here

        // after doing this, save the game!
        updateGame(updatedGame)
        Log.i("Updated game!", updatedGame.title)
        return updatedGame
    }
}