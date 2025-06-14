package com.example.collectalogger2.ui.wishlist

import com.example.collectalogger2.data.repository.GameWishlistRepository
import com.example.collectalogger2.data.WishlistGame
import com.example.collectalogger2.util.Filter
import com.example.collectalogger2.util.Sort
import com.example.collectalogger2.util.SortBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WishlistUiState(
    // TODO in the future, these should be WishSort and WishFilter, having more filters/sorts like for price.
    val sort: Sort? = Sort(SortBy.RELEASED, false),
    val filter: Filter? = null,
    val games: List<WishlistGame>
)

class WishListViewModel {
    val fakeRepository = GameWishlistRepository()
    private val _uiState = MutableStateFlow(WishlistUiState(games = fakeRepository.getFakeWishlistGames()))
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

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
