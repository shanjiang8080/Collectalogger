package com.example.collectalogger2.ui.detail

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.Genre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(val container: AppContainer, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _game = MutableStateFlow<Game?>(null)
    private val _currentDialog = MutableStateFlow<String>("")
    val game = _game.asStateFlow()
    private val _gameGenres = MutableStateFlow<List<Genre>>(emptyList())
    val gameGenres = _gameGenres.asStateFlow()

    private val _allGameGenres = MutableStateFlow<List<Genre>>(emptyList())
    val allGameGenres = _allGameGenres.asStateFlow()
    val currentDialog = _currentDialog.asStateFlow()
    val gameId: Long = checkNotNull(savedStateHandle["id"])
    init {
        viewModelScope.launch(Dispatchers.IO) {
            container.gameLibraryRepository.getGameStream(gameId).collect { flowGame: Game? ->
                if (flowGame != null) {
                    _game.value = flowGame
                    var newGenres = mutableListOf<Genre>()
                    flowGame.genre.forEach { genreId ->
                        var genre = container.gameLibraryRepository.getGenreByIGDBId(genreId)
                        if (genre != null) newGenres.add(genre)
                    }
                    _gameGenres.value = newGenres.toList()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            container.gameLibraryRepository.genreFlow.collect { genres ->
                _allGameGenres.value = genres
            }
        }
    }


    fun setDialog(newValue: String) {
        _currentDialog.value = newValue
    }

    fun editPlayStatus(newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            container.gameLibraryRepository.updateGame(_game.value!!.copy(status = newStatus))
        }
    }

    fun toggleFavoriteGame() {
        viewModelScope.launch(Dispatchers.IO) {
            container.gameLibraryRepository.updateGame(_game.value!!.copy(isFavorite = !_game.value!!.isFavorite))
            Log.i("DetailViewModel", "${_game.value!!.title} is now ${if (_game.value!!.isFavorite) "" else "un"}favorited.")
        }
    }

    fun onSubmitChanges(
        title: String,
        sortingName: String,
        description: String,
        developers: Set<String>,
        publishers: Set<String>,
        genres: List<Genre>
    ) {
        var newGenres = mutableListOf<Int>()
        genres.forEach { genre ->
            newGenres.add(genre.igdbId)
        }

        viewModelScope.launch(Dispatchers.IO) {
            container.gameLibraryRepository.updateGame(
                _game.value!!.copy(
                    title = title,
                    sortingName = sortingName,
                    description = description,
                    // for developers/publishers, remove empty strings
                    developers = developers.minus(""),
                    publishers = publishers.minus(""),
                    genre = newGenres.toSet()
                )
            )
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