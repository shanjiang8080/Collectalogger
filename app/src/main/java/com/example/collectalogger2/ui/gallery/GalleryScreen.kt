package com.example.collectalogger2.ui.gallery

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.collectalogger2.navigation.Gallery
import com.example.collectalogger2.data.Game
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import com.example.collectalogger2.R
import kotlinx.coroutines.runBlocking


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onNavigateToDetail: (id: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyGridState.Saver) {
        LazyGridState()
    }
    // eventually, the uiState can be changed by filters.
    // not now, though.

    Scaffold(
        topBar = {

        },
        floatingActionButton = {
            FloatingRefreshButton(viewModel)
        },
    ) {
        val itemModifier = Modifier
            .width(80.dp)
            .border(1.dp, Color.Blue)
            .wrapContentSize()
        // eventually, make the size changeable via a setting!
        LazyVerticalGrid(
            columns = GridCells.Adaptive(133.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            state = listState
        ) {
            items(uiState.games) {
                GalleryGame(it, onNavigateToDetail)
            }
        }
    }

}

@Composable
fun FloatingRefreshButton(viewModel: GalleryViewModel) {
    FloatingActionButton(
        onClick = { viewModel.updateGames() },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.secondary
    ) {
        Icon(Icons.Filled.Refresh, "Update games button.")
    }
}

@Composable
fun GalleryGame(
    game: Game,
    onNavigateToDetail: (id: Long) -> Unit,
) {
    val gameModifier = Modifier
        .clickable(onClick = { onNavigateToDetail(game.id) })
        .size(width = 133.dp, height = 177.dp)
    if (game.imageUrl != "") {
        AsyncImage(
            model = game.imageUrl,
            contentDescription = game.title,
            // add modifier = Modifier.clickable
            // to make it work!
            modifier = gameModifier
        )
    } else {
        Box(
            modifier = gameModifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.not_found),
                contentDescription = game.title,
                modifier = Modifier.fillMaxSize()
            )
            Text(text = game.title, color = Black)
        }
    }
}

@Preview(
    widthDp = 300,
    heightDp = 600,
    showBackground = true
)
@Composable
fun GalleryGamePreview() {
    val game = Game("Penny's Big Breakaway", 10)
    game.imageUrl = "https://upload.wikimedia.org/wikipedia/en/a/a0/Penny%27s_Big_Breakaway_box_art.jpg"
    GalleryGame(game, {})

}
@Preview(
    showBackground = true
)
@Composable
fun GalleryGameNoImagePreview() {
    val game = Game("Rogue Legacy II", 19)
    GalleryGame(game, {})
}

