package com.example.collectalogger2.ui.gallery

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.AppDataContainer
import com.example.collectalogger2.CollectaloggerApplication
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDatabase
import com.example.collectalogger2.data.repository.GameLibraryRepository
import com.example.collectalogger2.util.Filter
import com.example.collectalogger2.util.Sort
import com.example.collectalogger2.util.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log
import androidx.lifecycle.ViewModelProvider

data class GalleryUiState(
    val sort: Sort? = Sort(SortBy.RELEASED, false), // TODO Filter is not implemented
    val filter: Filter? = null, // TODO Filter is not implemented
    val games: List<Game> = emptyList()
)

class GalleryViewModel(val container: AppContainer) : ViewModel() {
    // TODO eventually, pass in the user data in the GameLibraryRepository
    // like, for example, Steam account ID
    var repository = container.gameLibraryRepository

    private var cachedGames: List<Game>? = null

    fun loadGames() {
        // get cached data, if available
        cachedGames?.let { games ->
            _uiState.update { it.copy(games = games) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val gamesFromDb = repository.getAllGames()
            cachedGames = gamesFromDb
            _uiState.update { it.copy(games = gamesFromDb) }
        }
    }

    private lateinit var _games: List<Game>
    private var _uiState = MutableStateFlow(GalleryUiState())
    init {
        loadGames()
    }
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

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
        }
    }
}

class GalleryViewModelFactory(
    private val container: AppContainer
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GalleryViewModel(container) as T
    }
}
