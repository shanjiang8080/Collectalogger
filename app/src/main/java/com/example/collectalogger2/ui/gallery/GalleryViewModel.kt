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
import com.example.collectalogger2.data.repository.RepositoryEvent
import com.example.collectalogger2.ui.gallery.DialogActionType.LoggedOut
import com.example.collectalogger2.ui.gallery.SnackbarActionType.NonImportedGames
import com.example.collectalogger2.ui.gallery.UiEvent.LoadingFinished
import com.example.collectalogger2.ui.gallery.UiEvent.ShowDialog
import com.example.collectalogger2.ui.gallery.UiEvent.ShowError
import com.example.collectalogger2.ui.gallery.UiEvent.ShowSnackbar
import com.example.collectalogger2.ui.settings.getEpicLogin
import com.example.collectalogger2.ui.settings.getSteamLogin
import com.example.collectalogger2.util.Filter
import com.example.collectalogger2.util.Sort
import com.example.collectalogger2.util.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val sort: Sort? = Sort(SortBy.RELEASED, false), // TODO Sort is not implemented
    val filter: Filter? = null,
    val games: List<Game> = emptyList()
)

sealed class SnackbarActionType {
    data class NonImportedGames(val items: MutableMap<String, List<Game>>) : SnackbarActionType()
    object Info : SnackbarActionType()
}

sealed class DialogActionType {
    data class LoggedOut(val library: String) : DialogActionType()
    data class CheckNonImportedItems(val items: Map<String, List<Game>>) : DialogActionType()
}

// TODO implement a UiEvent thing and make it work properly as snackbars/dialogs
// snackbars for informational/low priority stuff like retrying, errors, etc
// also have one for missing games (but add the option for ignore lists in the future...)
// dialogs for high priority (probably?) like logged out of epic games
sealed class UiEvent {
    data class ShowSnackbar(val message: String, val actionType: SnackbarActionType) : UiEvent()
    data class ShowError(val message: String) : UiEvent()
    data class ShowDialog(val message: String, val actionType: DialogActionType) : UiEvent()
    object LoadingFinished : UiEvent()
}

class GalleryViewModel(val container: AppContainer, savedStateHandle: SavedStateHandle) :
    ViewModel() {
    // like, for example, Steam account ID
    var repository = container.gameLibraryRepository

    private val _cachedGames = MutableStateFlow<List<Game>>(emptyList())
    val cachedGames: StateFlow<List<Game>> = _cachedGames

    private var _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _allGameGenres = MutableStateFlow<List<Genre>>(emptyList())
    val allGameGenres = _allGameGenres.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    val loadPercentage = container.gameLibraryRepository.loadPercentage

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

        viewModelScope.launch(Dispatchers.Main) {
            container.gameLibraryRepository.eventFlow.collect { event ->
                when (event) {
                    is RepositoryEvent.ShowLoggedOut -> {
                        _uiEvents.emit(
                            ShowDialog(
                                "${event.dataSource.libraryName} session expired.",
                                LoggedOut(event.dataSource.libraryName)
                            )
                        )
                    }
                    is RepositoryEvent.ShowErrorMessage -> {
                        _uiEvents.emit(ShowError(event.message))
                    }
                    is RepositoryEvent.ShowInfoMessage -> {
                        _uiEvents.emit(ShowSnackbar(event.message, SnackbarActionType.Info))
                    }
                    is RepositoryEvent.ShowMissingGames -> {
                        _uiEvents.emit(
                            ShowSnackbar(
                                "Some games failed to automatically import",
                                NonImportedGames(event.gameMap)
                            )
                        )
                    }
                    is RepositoryEvent.ShowLoadingFinished -> {
                        _uiEvents.emit(LoadingFinished)
                    }
                }
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
    // UPDATE WHEN ADDING LIBRARIES
    fun saveSteamId(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getSteamLogin(url, container)
        }
    }

    fun saveEpicInfo(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getEpicLogin(code, container)
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
