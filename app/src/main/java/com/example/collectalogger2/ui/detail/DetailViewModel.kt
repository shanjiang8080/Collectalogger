package com.example.collectalogger2.ui.detail

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.data.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(val container: AppContainer, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _game = MutableStateFlow<Game?>(null)
    private val _currentDialog = MutableStateFlow<String>("")
    val game = _game.asStateFlow()
    val currentDialog = _currentDialog.asStateFlow()
    val gameId: Long = checkNotNull(savedStateHandle["id"])
    init {
        viewModelScope.launch(Dispatchers.IO) {
            _game.value = container.gameLibraryRepository.getGameById(gameId)
            // make an API call if missing elements.
            if (game.value?.backgroundUrl == "") { // TODO add this functionality
                updateGame()
            }
        }
    }

    suspend fun updateGame() {
        _game.value = container.gameLibraryRepository.getAuxiliaryInformation(game.value as Game)
    }

    fun setDialog(newValue: String) {
        _currentDialog.value = newValue
    }

    fun editPlayStatus(newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _game.value = _game.value!!.copy(status = newStatus)
            container.gameLibraryRepository.updateGame(_game.value!!)
        }
    }

}

@Suppress("UNCHECKED_CAST")
class DetailViewModelFactory(
    private val container: AppContainer,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return DetailViewModel(container, handle) as T
    }
}