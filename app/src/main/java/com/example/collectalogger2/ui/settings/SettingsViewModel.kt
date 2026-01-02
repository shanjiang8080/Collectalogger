package com.example.collectalogger2.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.collectalogger2.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class SettingsViewModel(val container: AppContainer) : ViewModel() {
    private val _steamId = MutableStateFlow<String>("")
    private val _epicInfo = MutableStateFlow<String>("")
    private val _currentStoreFront = MutableStateFlow<String>("")
    private val _gogUsername = MutableStateFlow<String>("")
    val currentStoreFront = _currentStoreFront.asStateFlow()
    val steamId = _steamId.asStateFlow()
    val epicInfo = _epicInfo.asStateFlow()
    val gogUsername = _gogUsername.asStateFlow()

    init {
        viewModelScope.launch {
            _steamId.value = container.settingsRepository.steamId.first()
            _epicInfo.value = container.settingsRepository.epicIdInfo.first()
            _gogUsername.value = container.settingsRepository.gogUsername.first()
            Log.i("steamid_open", steamId.value)
            Log.i("epicinfo_open", epicInfo.value)
            Log.i("gogusername_open", gogUsername.value)

            container.settingsRepository.steamId.collect { id ->
                _steamId.value = id
                Log.i("steamid", id)
            }
            container.settingsRepository.epicIdInfo.collect { id ->
                _epicInfo.value = id
                Log.i("epicinfo", id)
            }
            container.settingsRepository.gogUsername.collect { id ->
                _gogUsername.value = id
                Log.i("gogusername", id)
            }
        }
    }


    /**
     * Saves the SteamID to the settings given an input URL.
     */
    fun saveSteamId(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getSteamLogin(url, container)
        }
    }
    /**
     * Saves the Epic login info to the settings given the code.
     */
    fun saveEpicInfo(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getEpicLogin(code, container)
        }
    }

    /**
     * Saves the GOG username to the settings given the code.
     */
    fun saveGogUsername(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getGogLogin(username, container)
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
