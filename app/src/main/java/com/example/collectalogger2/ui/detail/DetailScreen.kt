package com.example.collectalogger2.ui.detail

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.collectalogger2.R
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.util.PlayStatus
import kotlin.math.floor

// TODO add a back button at the top left corner
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,
    onEditPlayStatus: (String) -> Unit
) {

    val realGame by viewModel.game.collectAsStateWithLifecycle()
    val currentDialog by viewModel.currentDialog.collectAsStateWithLifecycle()

    realGame?.let {
        DetailScreenBody(realGame!!, onNavigateBack, currentDialog, {it -> viewModel.setDialog(it) }, onEditPlayStatus)
    } ?: run {
        // TODO add a loading bar or something.
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun DetailScreenBody(
    game: Game,
    onNavigateBack: () -> Unit,
    currentDialog: String,
    setDialog: (String) -> Unit,
    onEditPlayStatus: (String) -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Scaffold (
        topBar = {
            TopAppBar(
                title = {
                    Text("")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back button",
                            modifier = Modifier.height(48.dp)
                        )
                    }
                },
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp
                ),
                )
        },
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            BackgroundImage(screenHeight, game)

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    CoverArt(game, screenWidth, Modifier
                        .padding(top = screenHeight / 11, start = 15.dp)
                        .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .padding(top = 65.dp)
                    ) {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(10.dp)
                        )


                    }

                }
                // TODO put developer(s)/publisher(s) once that works out 
                // Card containing hours/play status
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        LabelIconCombo(
                            iconPainter = painterResource(R.drawable.mic_schedule),
                            iconDescription = "Time played icon",
                            textLabel = "Time Played",
                            textValue = if (game.playTime > 0) "${String.format("%.1f",game.playTime / 60f)} hours" else "Unplayed",
                            textAction = { setDialog("TimePlayed") }
                        )
                        LabelIconCombo(
                            iconPainter = choosePlayStatusIcon(game),
                            iconDescription = "Play status icon",
                            textLabel = "Play Status",
                            textValue = if (!game.status.isEmpty()) game.status else PlayStatus.Unplayed,
                            textAction = { setDialog("PlayStatus") }
                        )
                    }

                }
                // TODO add genres here
                Column(
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = game.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                // TODO add images carousel here


            }




        }
        when (currentDialog) {
            "TimePlayed" -> { PlayTimeDialog(game.playTime, { setDialog("") })}
            "PlayStatus" -> { PlayStatusDialog( game.status, { setDialog("") }, onEditPlayStatus) }
        }

    }
}

@Preview
@Composable
fun PlayStatusDialogPreview() {
    PlayStatusDialog(PlayStatus.Beaten, {}, {})
}

@Preview
@Composable
fun PlayTimeDialogPreview() {
    PlayTimeDialog(3456, {})
}

@Composable
fun PlayTimeDialog(
    time: Long,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Time Played",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic_schedule),
                        contentDescription = "Time played icon",
                        modifier = Modifier.size(32.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth()
                    ) {

                        BigHighlightHorizontalLabel(
                            textBig = "${floor((time / 60f)).toLong()}",
                            textSmall = "hours"
                        )
                        BigHighlightHorizontalLabel(
                            textBig = "${time % 60}",
                            textSmall = "minutes"
                        )
                    }

                }
            }
        }
    }
}

@Composable
fun BigHighlightHorizontalLabel(
    textBig: String,
    textSmall: String
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = textBig,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "  $textSmall",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PlayStatusDialog(
    gameStatus: String,
    onDismiss: () -> Unit,
    onEditPlayStatus: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(20.dp)
            ) {
                Text(
                    text = "Edit Play Status",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val radioOptions = listOf(
                    PlayStatus.Unplayed,
                    PlayStatus.Abandoned,
                    PlayStatus.Planned,
                    PlayStatus.Played,
                    PlayStatus.Playing,
                    PlayStatus.Beaten,
                    PlayStatus.Completed,
                )
                val (selectedOption, onOptionSelected) = remember { mutableStateOf(gameStatus) }

                Column(modifier = Modifier.selectableGroup()) {
                    radioOptions.forEach { text ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (text == selectedOption),
                                    onClick = { onOptionSelected(text) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (text == selectedOption),
                                onClick = null
                            )
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                // do a Submit and cancel button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Button(
                        colors = ButtonDefaults.buttonColors(),
                        onClick = {
                            // set the ID appropriately!
                            onEditPlayStatus(selectedOption)
                            onDismiss()
                        }
                    ) {
                        Text("Save")
                    }
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        onClick = {
                            onDismiss()
                        }) {
                        Text("Cancel")
                    }

                }


            }
        }
    }
}

@Preview(widthDp = 320,heightDp = 800)
@Composable
fun DetailScreenPreview() {
    val game = Game(
        title = "Stardew Valley",
        description = "Stardew Valley is an open-ended country-life RPG! You’ve inherited your grandfather’s old farm plot in Stardew Valley. Armed with hand-me-down tools and a few coins, you set out to begin your new life. Can you learn to live off the land and turn these overgrown fields into a thriving home? It won’t be easy. Ever since Joja Corporation came to town, the old ways of life have all but disappeared. The community center, once the town’s most vibrant hub of activity, now lies in shambles. But the valley seems full of opportunity. With a little dedication, you might just be the one to restore Stardew Valley to greatness!",
        sortingName = "Stardew Valley",
        status = PlayStatus.Beaten,
        imageUrl = "",
        backgroundUrl = "",
        playTime = 22255
    )
    DetailScreenBody(
        game = game,
        onNavigateBack = {},
        currentDialog = "",
        setDialog = {},
        onEditPlayStatus = {})
}

@Composable
fun BackgroundImage(
    screenHeight: Dp,
    game: Game
) {
    val imageModifier = Modifier
        .height(screenHeight / 3.5f)
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
fun LabelIconCombo(
    iconPainter: Painter,
    iconDescription: String,
    textLabel: String,
    textValue: String,
    textAction: () -> Unit = {}
    ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(10.dp)
            .clickable(enabled = true, onClick = textAction, interactionSource = remember { MutableInteractionSource() }, indication = null)
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = iconDescription,
            tint = MaterialTheme.colorScheme.secondary
        )
        Column {
            Text(
                text = textLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = textValue,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black
            )
        }
    }
}


@Composable
fun CoverArt(game: Game, deviceWidth: Dp, modifier: Modifier) {
    val imageModifier = modifier
        .height(deviceWidth / 1.875f)
        .width(deviceWidth / 2.5f)
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
@Composable
private fun choosePlayStatusIcon(game: Game): Painter {
    return when (game.status) {
        PlayStatus.Unplayed -> painterResource(id = R.drawable.mic_circle_blank)
        "" -> painterResource(id = R.drawable.mic_circle_blank)
        PlayStatus.Playing -> painterResource(id = R.drawable.mic_circle_partial_half)
        PlayStatus.Played -> painterResource(id = R.drawable.mic_circle_partial)
        PlayStatus.Beaten -> painterResource(id = R.drawable.mic_check_circle)
        PlayStatus.Completed -> painterResource(id = R.drawable.mic_check_circle_large)
        PlayStatus.Planned -> painterResource(id = R.drawable.mic_circle_dot)
        PlayStatus.Abandoned -> painterResource(id = R.drawable.mic_circle_crossed)
        else -> throw Exception("Play status should not be anything else!")
    }
}
