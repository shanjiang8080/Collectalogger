package com.example.collectalogger2

import android.app.Application
import androidx.work.Configuration
import com.example.collectalogger2.data.workers.UpdateOwnedGamesWorkerFactory

/**
 * Basically a handle for the environment of the app.
 * Gives access to resources, system services, app-level information.
 */
class CollectaloggerApplication : Application(), Configuration.Provider {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(UpdateOwnedGamesWorkerFactory(container.gameLibraryRepository))
                .build()
}