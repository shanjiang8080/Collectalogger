package com.example.collectalogger2.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.EpicSource
import com.example.collectalogger2.util.SteamSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject


class SettingsViewModel(val container: AppContainer) : ViewModel() {
    private val _steamId = MutableStateFlow<String>("")
    private val _epicInfo = MutableStateFlow<String>("")
    private val _currentStoreFront = MutableStateFlow<String>("")
    val currentStoreFront = _currentStoreFront.asStateFlow()
    val steamId = _steamId.asStateFlow()
    val epicInfo = _epicInfo.asStateFlow()
    init {
        viewModelScope.launch {
            container.settingsRepository.steamId.collect { id ->
                _steamId.value = id
            }
            container.settingsRepository.epicIdInfo.collect { id ->
                _epicInfo.value = id
            }
        }
    }

    /**
     * This function takes in a URL like https://steamcommunity.com/id/personnotman/
     * and converts it to the steamID.
     */
    suspend fun getSteamID(url: String): String {
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

    /**
     * Saves the SteamID to the settings given an input URL.
     * Returns true if successful, false if not.
     */
    fun saveSteamId(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id: String
            try {
                id = getSteamID(url)
                container.settingsRepository.saveSteamId(id)
                Log.i("Steam ID saved!", id)
            } catch (e: Exception) {
                Log.e("Failed to process Steam ID from URL!", e.message ?: "")
            }
        }
    }
    /**
     * Saves the Epic login info to the settings given the code.
     * Returns true if successful, false if not.
     */
    fun saveEpicInfo(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
                container.settingsRepository.saveEpicIdInfo(response.toString())
                Log.i("Epic login info saved!", response.toString())
            } catch (ex: Exception) {
                Log.e("Failed to save Epic Games info!", ex.message ?: "")
            }
        }
    }

    fun setStoreFront(newValue: String) {
        _currentStoreFront.value = newValue
    }
}

@Suppress("UNCHECKED_CAST")
class SettingsViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(container) as T
    }
}
