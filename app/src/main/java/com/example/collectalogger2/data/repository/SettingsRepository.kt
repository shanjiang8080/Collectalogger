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
        val EPIC_ID_INFO = stringPreferencesKey("epic_id_info")
        val GOG_USERNAME = stringPreferencesKey("gog_username")
    }
    val steamId: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[STEAM_ID] ?: ""
        }
    val epicIdInfo: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[EPIC_ID_INFO] ?: ""
        }

    val gogUsername: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[GOG_USERNAME] ?: ""
        }

    suspend fun saveSteamId(id: String) {
        dataStore.edit { it[STEAM_ID] = id }
    }
    suspend fun saveEpicIdInfo(info: String) {
        dataStore.edit { it[EPIC_ID_INFO] = info }
    }
    suspend fun saveGogUsername(username: String) {
        dataStore.edit { it[GOG_USERNAME] = username }
    }
}