package com.example.collectalogger2.data.repository

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.LocalDataSource
import com.example.collectalogger2.data.datasource.RemoteLibraryDataSource
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.APIStatusException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch


// I should replace the `remoteLibraryDataSource` with a list of them
class GameLibraryRepository(
    private val remoteLibraryDataSources: List<RemoteLibraryDataSource>,
    private val localDataSource: LocalDataSource?,
    private val gameDao: GameDao
) {
    fun getAllGamesStream(): Flow<List<Game>> = gameDao.getAllGamesStream()

    fun getAllGames(): List<Game> = gameDao.getAllGames()

    fun getGameStream(id: Long): Flow<Game?> = gameDao.getGameStream(id)

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
                                val newGame = game.copy(id = existingGame.id)
                                updateGame(newGame)
                            } else {
                                // add the new game
                                insertGame(game)
                            }
                        }
                }
                catch (e: Exception) {
                    if (e is APIStatusException) {
                        Log.e("APIStatusException happened with HTTP code ${e.statusCode}", e.message ?: "")
                    } else if (e is APIException) {
                        Log.e("APIException happened", e.message ?: "")
                    } else {
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

    // TODO Delete this when the thing starts working
    fun getFakeGames(): List<Game> {
        val g1 = Game("Penny's Big Breakaway", 1)
        g1.imageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co6nmg.webp"
        val g2 = Game("Sonic Mania", 2)
        g2.imageUrl = "https://images.igdb.com/igdb/image/upload/t_cover_big/co1obi.webp"
        val g3 = Game("Rogue Legacy", 3)
        return listOf(g1, g2, g3)
    }

}