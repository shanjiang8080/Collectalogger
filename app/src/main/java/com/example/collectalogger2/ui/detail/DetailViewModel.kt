package com.example.collectalogger2.ui.detail

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.CollectaloggerApplication
import com.example.collectalogger2.data.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(val container: AppContainer, val gameId: Long) : ViewModel() {
    var repository = container.gameLibraryRepository
    private val _game = MutableStateFlow<Game?>(null)
    val game = _game.asStateFlow()
    init {
        // load the game
        viewModelScope.launch(Dispatchers.IO) {
            _game.value = container.gameLibraryRepository.getGameById(gameId)
        }
        // make an API call if missing elements.
        if (false) { // TODO add this functionality
            updateGame()
        }
    }

    fun updateGame() {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO not implemented.
        }
    }
}
