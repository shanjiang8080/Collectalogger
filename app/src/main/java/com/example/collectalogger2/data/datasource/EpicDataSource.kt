package com.example.collectalogger2.data.datasource

import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import kotlinx.coroutines.flow.Flow

class EpicDataSource(gameDao: GameDao) : RemoteLibraryDataSource(gameDao) {
    override var libraryName: String = "Epic Games"

    override suspend fun getGames(forceUpdate: Boolean): Flow<Game> {
        TODO("Not yet implemented")
    }
}