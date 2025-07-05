package com.example.collectalogger2.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.collectalogger2.R
import com.example.collectalogger2.data.Game

// TODO add a back button at the top left corner
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
) {

    val realGame by viewModel.game.collectAsStateWithLifecycle()

    realGame?.let {
        DetailScreenBody(realGame!!)
    } ?: run {
        // TODO add a loading bar or something.
    }

}

@Composable
fun DetailScreenBody(game: Game) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        BackgroundImage(screenHeight, game)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .padding(top = screenHeight / 3)
                .fillMaxWidth()
        ) {
            Column(
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .padding(top = 65.dp)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                            )

                        Text(
                            text = "Time Played: ${String.format("%.1f", game.playTime / 60f)} hours",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = game.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        CoverArt(game, screenWidth, Modifier
            .padding(top = screenHeight / 11)
            .align(Alignment.TopCenter)
            .wrapContentSize(unbounded = true, align = Alignment.TopStart)
        )
    }
}

@Preview(
    widthDp = 320,
    heightDp = 640,
)
@Composable
fun DetailScreenPreview() {
    val game = Game(
        title = "Stardew Valley",
        description = "Stardew Valley is an open-ended country-life RPG! You’ve inherited your grandfather’s old farm plot in Stardew Valley. Armed with hand-me-down tools and a few coins, you set out to begin your new life. Can you learn to live off the land and turn these overgrown fields into a thriving home? It won’t be easy. Ever since Joja Corporation came to town, the old ways of life have all but disappeared. The community center, once the town’s most vibrant hub of activity, now lies in shambles. But the valley seems full of opportunity. With a little dedication, you might just be the one to restore Stardew Valley to greatness!",
        sortingName = "Stardew Valley",
        imageUrl = "",
        backgroundUrl = "",
        playTime = 22255
    )
    DetailScreenBody(game = game)
}

@Composable
fun BackgroundImage(
    screenHeight: Dp,
    game: Game
) {
    val imageModifier = Modifier
        .height(screenHeight / 2)
        .fillMaxWidth()
        .blur(5.dp) // NOTE: Only works on Android 12+
    if (game.backgroundUrl != "") {
        AsyncImage(
            model = game.backgroundUrl,
            contentDescription = "Background art of ${game.title}",
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.background_placeholder),
            contentDescription = "Placeholder background",
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )
    }
}

@Composable
fun CoverArt(game: Game, deviceWidth: Dp, modifier: Modifier) {
    val imageModifier = modifier
        .height(deviceWidth / 1.5f)
        .width(deviceWidth / 2)
        .clip(RoundedCornerShape(15.dp))
    if (game.imageUrl != "") {
        AsyncImage(
            model = game.imageUrl,
            contentDescription = "Cover image",
            modifier = imageModifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.not_found),
            contentDescription = "Placeholder cover",
            modifier = imageModifier,
            contentScale = ContentScale.Fit
        )
    }
}