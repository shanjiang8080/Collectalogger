package com.example.collectalogger2.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
            contentDescription = stringResource(R.string.background_art_cd, game.title),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.background_placeholder),
            contentDescription = stringResource(R.string.placeholder_background_cd),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
