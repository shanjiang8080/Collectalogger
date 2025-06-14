package com.example.collectalogger2.data.repository

import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.datasource.LocalDataSource
import com.example.collectalogger2.data.datasource.RemoteLibraryDataSource
import kotlinx.coroutines.flow.Flow


// I should replace the `remoteLibraryDataSource` with a list of them
class GameLibraryRepository(
    private val remoteLibraryDataSources: List<RemoteLibraryDataSource>,
    private val localDataSource: LocalDataSource?,
    private val gameDao: GameDao
) {
    fun getAllGamesStream(): Flow<List<Game>> = gameDao.getAllGames()

    fun getGameStream(id: Long): Flow<Game?> = gameDao.getGame(id)

    fun getGameStreamByIGDBId(IGDBId: Long): Flow<Game?> = gameDao.getGameByIGDBId(IGDBId)

    suspend fun insertGame(game: Game) = gameDao.insert(game)

    suspend fun deleteGame(game: Game) = gameDao.delete(game)

    suspend fun updateGame(game: Game) = gameDao.update(game)

    suspend fun updateGameLibraries() {
        for (dataSource in remoteLibraryDataSources) {
            val games = dataSource.getGames()
            games.collect { game ->
                // See if it's here, if there is one
                val existingGame = gameDao.getGameByIGDBId(game.igdbId)
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