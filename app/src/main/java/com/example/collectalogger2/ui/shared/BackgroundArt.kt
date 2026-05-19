package com.example.collectalogger2.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import com.example.collectalogger2.R
import com.example.collectalogger2.data.Game

/**
 * A component that shows the background art of a game.
 */
@Composable
fun BackgroundArt(
    modifier: Modifier = Modifier,
    game: Game
) {
    if (game.backgroundUrl != "") {
        AsyncImage(
            model = game.backgroundUrl,
            contentDescription = "Background art of ${game.title}",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.background_placeholder),
            contentDescription = "Placeholder background",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
