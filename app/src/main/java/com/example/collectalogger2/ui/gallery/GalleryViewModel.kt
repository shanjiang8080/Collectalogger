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

data class GalleryUiState(
    val sort: Sort? = Sort(SortBy.RELEASED, false),
    val filter: Filter? = null,
    val games: List<Game> = emptyList()
)

class GalleryViewModel(val container: AppContainer) : ViewModel() {
    // TODO eventually, pass in the user data in the GameLibraryRepository
    // like, for example, Steam account ID
    var repository = container.gameLibraryRepository

    private val _games: List<Game> = emptyList()
    private val _uiState = MutableStateFlow(GalleryUiState(games = _games))
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val games = repository.getAllGames()
            _uiState.value = _uiState.value.copy(games = games)
            // Log.i("Games list: ", "${repository.getAllGames()}")
        }
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