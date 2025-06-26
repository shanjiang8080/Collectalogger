package com.example.collectalogger2

import android.content.Context
import com.example.collectalogger2.data.GameDatabase
import com.example.collectalogger2.data.datasource.LocalDataSource
import com.example.collectalogger2.data.datasource.RemoteLibraryDataSource
import com.example.collectalogger2.data.datasource.SteamDataSource
import com.example.collectalogger2.data.repository.GameLibraryRepository

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val gameLibraryRepository: GameLibraryRepository
}

/**
 * [AppContainer] implementation that provides instance of [GameLibraryRepository]
 * TODO will contain other repositories later, like Wishlists.
 */
class AppDataContainer(private val context: Context) : AppContainer {
    /**
     * Implementation for [GameLibraryRepository]
     */
    override val gameLibraryRepository: GameLibraryRepository by lazy {
        val gameDao = GameDatabase.getDatabase(context).gameDao()
        GameLibraryRepository(
            remoteLibraryDataSources = emptyList<RemoteLibraryDataSource>()
                .plus(SteamDataSource("76561198424115282", gameDao)), // Can add more DataSources later,
            // Also, TODO create a DAO for platform (e.g: Steam) user IDs
            localDataSource = LocalDataSource(),
            gameDao = gameDao
        )
    }
}
