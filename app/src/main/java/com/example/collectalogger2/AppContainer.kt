package com.example.collectalogger2

import android.content.Context
import com.example.collectalogger2.data.GameDatabase
import com.example.collectalogger2.data.datasource.EpicDataSource
import com.example.collectalogger2.data.datasource.GenreDataSource
import com.example.collectalogger2.data.datasource.GogDataSource
import com.example.collectalogger2.data.datasource.ItchDataSource
import com.example.collectalogger2.data.datasource.LocalDataSource
import com.example.collectalogger2.data.datasource.RemoteLibraryDataSource
import com.example.collectalogger2.data.datasource.SteamDataSource
import com.example.collectalogger2.data.repository.GameLibraryRepository
import com.example.collectalogger2.data.repository.SettingsRepository
import com.example.collectalogger2.util.dataStore

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val settingsRepository: SettingsRepository
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
    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            dataStore = context.dataStore
        )
    }
    override val gameLibraryRepository: GameLibraryRepository by lazy {
        val gameDao = GameDatabase.getDatabase(context).gameDao()
        val genreDao = GameDatabase.getDatabase(context).genreDao()

        GameLibraryRepository(
            remoteLibraryDataSources = emptyList<RemoteLibraryDataSource>()
                // UPDATE WHEN ADDING LIBRARIES
                .plus(
                    SteamDataSource(
                        userIdFlow = settingsRepository.steamId,
                        gameDao = gameDao
                    )
                )
                .plus(EpicDataSource(
                    userInfoFlow = settingsRepository.epicIdInfo,
                    userInfoSetter = { str -> settingsRepository.saveEpicIdInfo(str) },
                    gameDao = gameDao
                )
                )
                .plus(
                    GogDataSource(
                        usernameFlow = settingsRepository.gogUsername,
                        gameDao = gameDao
                    )
                )
                .plus(
                    ItchDataSource(
                        secretFlow = settingsRepository.itchSecret,
                        gameDao = gameDao
                    )
                ),
            localDataSource = LocalDataSource(),
            genreDataSource = GenreDataSource(),
            gameDao = gameDao,
            genreDao = genreDao
        )
    }
}
