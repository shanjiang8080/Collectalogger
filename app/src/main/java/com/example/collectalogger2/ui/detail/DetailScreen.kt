package com.example.collectalogger2.ui.detail

import android.R.attr.contentDescription
import android.media.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.collectalogger2.R
import com.example.collectalogger2.data.Game

// TODO add a back button at the top left corner
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    game: Game? = null,
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

    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundImage(screenHeight, game)

        Card(
            modifier = Modifier
                .padding(top = screenHeight / 3)
                .fillMaxHeight()
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
                            .padding(top = 60.dp)
                            .padding(10.dp)

                    ) {
                        Text(game.title, fontWeight = FontWeight.Black, fontSize = 30.sp)

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Description", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(game.description)
                        }
                    }
                }
            }
        }

        CoverArt(game, screenWidth, Modifier
            .padding(top = screenHeight / 10)
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
        backgroundUrl = ""
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