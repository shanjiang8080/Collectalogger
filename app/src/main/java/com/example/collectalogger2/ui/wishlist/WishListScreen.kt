package com.example.collectalogger2.ui.wishlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.collectalogger2.R
import com.example.collectalogger2.data.WishlistGame
import com.example.collectalogger2.navigation.WishList

@Composable
fun WishListScreen(
    viewModel: WishListViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(uiState.games) {
            WishListGame(it)
        }
    }
}

@Composable
fun WishListGame(
    game: WishlistGame
) {
    if (game.game.imageUrl != "") {
        AsyncImage(
            model = game.game.imageUrl,
            contentDescription = game.game.title
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.not_found),
            contentDescription = game.game.title
        )
    }
}
