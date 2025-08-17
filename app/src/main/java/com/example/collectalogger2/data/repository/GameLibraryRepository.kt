package com.example.collectalogger2.data.repository

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.LocalDataSource
import com.example.collectalogger2.data.datasource.RemoteLibraryDataSource
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.APIStatusException
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.PlayStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow


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

    fun getGamesBySearchTerm(search: String, limit: Int = 0): List<Game> =
        if (limit != 0)
            gameDao.getGamesSearchLimited(search, limit)
        else
            gameDao.getGamesSearch(search)

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
                                var newGame = game.copy(id = existingGame.id, playTime = highestPlaytime, source = existingGame.source.plus(game.source))

                                if (game.status.isEmpty()) {
                                    newGame = if (game.playTime > 0) {
                                        game.copy(status = PlayStatus.Played)
                                    } else {
                                        game.copy(status = PlayStatus.Unplayed)
                                    }
                                }
                                updateGame(newGame)
                                Log.i("Game updated!", "{title: ${game.title}, igdbId: ${game.igdbId}, id: ${game.id}}")
                            } else {
                                // set the basic play status based on playtime
                                var newGame = if (game.playTime > 0) {
                                    game.copy(status = PlayStatus.Played)
                                } else {
                                    game.copy(status = PlayStatus.Unplayed)
                                }
                                // add the new game
                                insertGame(newGame)
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

}