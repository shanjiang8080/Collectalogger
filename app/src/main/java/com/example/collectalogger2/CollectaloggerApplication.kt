package com.example.collectalogger2

import android.app.Application

/**
 * Basically a handle for the environment of the app.
 * Gives access to resources, system services, app-level information.
 */
class CollectaloggerApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}