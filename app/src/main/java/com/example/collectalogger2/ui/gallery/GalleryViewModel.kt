package com.example.collectalogger2.ui.gallery

import androidx.lifecycle.ViewModel
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.repository.GameLibraryRepository
import com.example.collectalogger2.util.Filter
import com.example.collectalogger2.util.Sort
import com.example.collectalogger2.util.SortBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GalleryUiState(
    val sort: Sort? = Sort(SortBy.RELEASED, false),
    val filter: Filter? = null,
    val games: List<Game>
)

class GalleryViewModel : ViewModel() {
    // TODO eventually, pass in the user data in the GameLibraryRepository
    // like, for example, Steam account ID
    val fakeRepository = GameLibraryRepository(null, null)
    private val _uiState = MutableStateFlow(GalleryUiState(games = fakeRepository.getFakeGames()))
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
}