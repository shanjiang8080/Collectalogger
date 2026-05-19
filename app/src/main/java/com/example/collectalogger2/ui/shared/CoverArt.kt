package com.example.collectalogger2.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.collectalogger2.R
import com.example.collectalogger2.data.Game
import java.io.File

/**
 * A component for game cover art. Covers have 3:4 aspect ratio.
 * If the game has a custom cover, that is used,
 * If not, the IGDB cover is used,
 * If that is not present, a default icon is displayed with the game's title overlayed.
 * Pass in a modifier for functionality like being clickable.
 */
@Composable
fun CoverArt(
    game: Game,
    modifier: Modifier = Modifier,
    displayText: Boolean = true,
) {
    if (game.customCover != "") {
        // Display custom cover
        AsyncImage(
            model = File(game.customCover),
            contentDescription = game.title,
            contentScale = ContentScale.FillBounds,
            modifier = modifier.aspectRatio(0.75f),
        )
    } else if (game.imageUrl != "") {
        // Display URL cover
        AsyncImage(
            model = game.imageUrl,
            contentDescription = game.title,
            contentScale = ContentScale.FillBounds,
            modifier = modifier.aspectRatio(0.75f),
        )
    } else {
        // Display the default cover with name
        val imageId: Int
        val textColor: Color
        // #46 change if you implement user theme changes
        if (isSystemInDarkTheme()) {
            imageId = if (displayText) R.drawable.text_not_found_dark else R.drawable.not_found_dark
            textColor = White
        } else {
            imageId =
                if (displayText) R.drawable.text_not_found_light else R.drawable.not_found_light
            textColor = Black
        }
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = imageId),
                contentDescription = game.title,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.aspectRatio(0.75f)
            )

            if (displayText) {
                Column(
                    modifier = Modifier.matchParentSize()
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    // This Box occupies the bottom 50% of the space
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = game.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = textColor,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun CoverArtPreview() {
    CoverArt(Game(title = "Test Title 123"))
}

@Preview
@Composable
private fun CoverArtPreviewNoText() {
    CoverArt(Game(title = "Test Title 123"), displayText = false)
}