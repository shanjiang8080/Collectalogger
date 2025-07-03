package com.example.collectalogger2.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private companion object {
        val STEAM_ID = stringPreferencesKey("steam_id")
    }
    val steamId: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[STEAM_ID] ?: ""
        }

    suspend fun saveSteamId(id: String) {
        dataStore.edit { it[STEAM_ID] = id }
    }
}