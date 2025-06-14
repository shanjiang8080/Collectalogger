package com.example.collectalogger2.ui.gallery

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.collectalogger2.R


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GalleryScreen(
    gallery: Gallery? = null,
    viewModel: GalleryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // eventually, the uiState can be changed by filters.
    // not now, though.

    Scaffold(
        topBar = {

        }
    ) {
        val itemModifier = Modifier
            .width(80.dp)
            .border(1.dp, Color.Blue)
            .wrapContentSize()
        // eventually, make the size changeable via a setting!
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(uiState.games) {
                GalleryGame(it)
            }
        }
    }

}

@Composable
fun GalleryGame(
    game: Game
) {
    if (game.imageUrl != "") {
        AsyncImage(
            model = game.imageUrl,
            contentDescription = game.title,
            // add modifier = Modifier.clickable
            // to make it work!
        )
    } else {
        Box(
            modifier = Modifier,
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
    GalleryGame(game)

}
@Preview(
    showBackground = true
)
@Composable
fun GalleryGameNoImagePreview() {
    val game = Game("Rogue Legacy II", 19)
    GalleryGame(game)
}

