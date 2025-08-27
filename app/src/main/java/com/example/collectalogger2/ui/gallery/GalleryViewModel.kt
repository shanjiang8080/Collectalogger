package com.example.collectalogger2.ui.gallery

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
import com.example.collectalogger2.util.Filter
import com.example.collectalogger2.util.Sort
import com.example.collectalogger2.util.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val sort: Sort? = Sort(SortBy.RELEASED, false), // TODO Sort is not implemented
    val filter: Filter? = null,
    val games: List<Game> = emptyList()
)

class GalleryViewModel(container: AppContainer, savedStateHandle: SavedStateHandle) : ViewModel() {
    // like, for example, Steam account ID
    var repository = container.gameLibraryRepository

    private val _cachedGames = MutableStateFlow<List<Game>>(emptyList())
    val cachedGames: StateFlow<List<Game>> = _cachedGames

    private var _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _allGameGenres = MutableStateFlow<List<Genre>>(emptyList())
    val allGameGenres = _allGameGenres.asStateFlow()

    private fun <T> getList(
        savedStateHandle: SavedStateHandle,
        key: String
    ): List<T>? {
        return savedStateHandle.get<T?>(key)?.let { listOf(it) }
    }

    init {
        // prepare filter
        // TODO in the future, for genre, developer, publisher, library, platform, split based on commas.
        val filter = Filter(
            library = getList(savedStateHandle, "library"),
            platform = getList(savedStateHandle, "platform"),
            genre = getList(savedStateHandle, "genre"),
            developer = getList(savedStateHandle, "developer"),
            publisher = getList(savedStateHandle, "publisher"),
            isFavorite = savedStateHandle.get<Boolean?>("isFavorite")
        )

        viewModelScope.launch(Dispatchers.IO) {
            container.gameLibraryRepository.getAllGamesStream().collect { games ->
                _cachedGames.value = games
                _uiState.update {
                    it.copy(
                        games = _cachedGames.value,
                        filter = filter
                    )
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            container.gameLibraryRepository.genreFlow.collect { genres ->
                _allGameGenres.value = genres
            }
        }
    }

    fun updateFilter(newFilter: Filter) {
        _uiState.update {
            it.copy(filter = newFilter)
        }
    }
    fun updateSort(newSort: Sort) {
        _uiState.update {
            it.copy(sort = newSort)
        }
    }

    fun updateGames() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGameLibraries()
            val games = repository.getAllGames()
            _uiState.value = _uiState.value.copy(games = games)
            _cachedGames.value = games
        }
    }
    // This is primarily for debugging
    fun deleteGames() {
        viewModelScope.launch(Dispatchers.IO) {
            var games = repository.getAllGames()
            games.forEach { game ->
                repository.deleteGame(game)
            }
            Log.i("GalleryViewModel", "All games purged from database!")
            _uiState.value = _uiState.value.copy(games = listOf<Game>())
            _cachedGames.value = listOf<Game>()
        }
    }


    fun getSearchedGames(search: String) {
        // instead of calling the database, do a filter of the cached games
        if (search.isEmpty()) {
            _uiState.value = _uiState.value.copy(games = _cachedGames.value)
            return
        }
        var filteredGames = cachedGames.value.filter { it.title.contains(search, ignoreCase = true) }
        _uiState.value = _uiState.value.copy(games = filteredGames)
    }

    fun getSearchedGamesList(search: String): List<Game> {
        if (search.isEmpty()) return listOf<Game>()
        var filteredGames = cachedGames.value.filter { it.title.contains(search, ignoreCase = true) }
        return filteredGames
    }

    fun restoreGames() {
        if (cachedGames.value.isEmpty()) {
            Log.w("GalleryViewModel", "cachedGames has 0 length")
        }
        _uiState.value = _uiState.value.copy(games = cachedGames.value)
    }
}

@Suppress("UNCHECKED_CAST")
class GalleryViewModelFactory(
    private val container: AppContainer,
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        return GalleryViewModel(container, handle) as T
    }
}
