package com.example.collectalogger2.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.libraryObjects.EpicSource
import com.example.collectalogger2.util.libraryObjects.SteamSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject


class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private companion object {
        val STEAM_ID = stringPreferencesKey("steam_id")
        val EPIC_ID_INFO = stringPreferencesKey("epic_id_info")
        val GOG_USERNAME = stringPreferencesKey("gog_username")
        val ITCH_SECRET = stringPreferencesKey("itch_secret")
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

    val itchSecret: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[ITCH_SECRET] ?: ""
        }
    suspend fun getSteamLogin(url: String) {
        val id: String
        try {
            id = _getSteamID(url)
            dataStore.edit { it[STEAM_ID] = id }
            saveSteamId(id)
            Log.i("Steam ID saved!", id)
        } catch (e: Exception) {
            Log.e("Failed to process Steam ID from URL!", e.message ?: "")
        }

    }

    suspend fun getEpicLogin(code: String) {
        try {
            // now we have the code, we can authenticate and get the string.
            val response = EpicSource.makeAPICall(
                domain = "account-public-service-prod03.ol.epicgames.com",
                path = "account/api/oauth/token",
                isGet = false,
                headerss = mapOf("Authorization" to "basic MzRhMDJjZjhmNDQxNGUyOWIxNTkyMTg3NmRhMzZmOWE6ZGFhZmJjY2M3Mzc3NDUwMzlkZmZlNTNkOTRmYzc2Y2Y="),
                params = mapOf(),
                bodyParams = mapOf("grant_type" to "authorization_code", "code" to code)
            ) as JSONObject
            saveEpicIdInfo(response.toString())
            Log.i("Epic login info saved!", response.toString())
        } catch (ex: Exception) {
            Log.e("Failed to save Epic Games info!", ex.message ?: "")
        }

    }

    suspend fun getGogLogin(username: String) {
        try {
            saveGogUsername(username)
            Log.i("GOG username saved!", username)
        } catch (e: Exception) {
            Log.e("Failed to save GOG username!", e.message ?: "")
        }
    }

    suspend fun getItchLogin(secret: String) {
        try {
            saveItchSecret(secret)
            Log.i("Itch secret saved!", secret)
        } catch (e: Exception) {
            Log.e("Failed to save Itch secret!", e.message ?: "")
        }
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
    suspend fun saveItchSecret(secret: String) {
        dataStore.edit { it[ITCH_SECRET] = secret }
    }
}

suspend fun _getSteamID(url: String): String {
    var newUrl = url
    if (newUrl.endsWith("/"))
        newUrl = newUrl.substring(0, newUrl.length - 1)

    // check to see if the url contains "profiles", since if it does,
    // it's using the old scheme with the ID
    if (newUrl.contains("profiles/")) {
        val startIndex = newUrl.indexOf("profiles/") + "profiles/".length
        newUrl = newUrl.substring(startIndex)
        // make an API call to ensure that the ID is valid:
        val response = SteamSource.makeAPICall(
            endpoint = "ISteamUser",
            endpoint2 = "GetUserGroupList",
            version = 1,
            params = mapOf("steamid" to newUrl)
        )
        if ((response.get("response") as JSONObject).has("success"))
            return newUrl
        throw APIException("URL $url does not correspond to a real Steam ID!")
    }

    // converts the vanityURL to the base thing
    if (newUrl.startsWith("https://"))
        newUrl = newUrl.substring(8)
    if (newUrl.startsWith("steamcommunity.com/"))
        newUrl = newUrl.substring(19)
    if (newUrl.startsWith("id/"))
        newUrl = newUrl.substring(3)

    // makes the API call to Steam
    val response = SteamSource.makeAPICall(
        endpoint = "ISteamUser",
        endpoint2 = "ResolveVanityURL",
        version = 1,
        params = mapOf("vanityurl" to newUrl)
    )
    return (response.get("response") as JSONObject).get("steamid") as String
}
