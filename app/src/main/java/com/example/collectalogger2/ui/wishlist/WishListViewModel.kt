package com.example.collectalogger2.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.data.repository.GameWishlistRepository
import com.example.collectalogger2.data.WishlistGame
import com.example.collectalogger2.ui.gallery.GalleryViewModel
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

// Has a container but is currently unused as database stuff isn't set up
// TODO Fix this at some point
class WishListViewModel(val container: AppContainer) : ViewModel() {
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

class WishListViewModelFactory(
    private val container: AppContainer
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WishListViewModel(container) as T
    }
}