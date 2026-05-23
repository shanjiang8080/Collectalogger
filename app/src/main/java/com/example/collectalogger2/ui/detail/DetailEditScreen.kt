package com.example.collectalogger2.ui.detail

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.collectalogger2.R
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.Genre
import com.example.collectalogger2.ui.shared.AnnotatedButton
import com.example.collectalogger2.ui.shared.BackgroundArt
import com.example.collectalogger2.ui.shared.CoverArt
import com.example.collectalogger2.util.PlayStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// TODO add a "hey you made changes but didn't save them, would you like to? popup"

@Composable
fun DetailEditScreen(
    viewModel: DetailViewModel,
    onNavigateBack: () -> Unit,

    ) {
    val realGame by viewModel.game.collectAsStateWithLifecycle()
    val gameGenres by viewModel.gameGenres.collectAsStateWithLifecycle()
    val allGameGenres by viewModel.allGameGenres.collectAsStateWithLifecycle()

    realGame?.let {
        DetailEditScreenBody(
            game = it,
            gameGenres = gameGenres,
            allGameGenres = allGameGenres,
            onNavigateBack = onNavigateBack,
            onSubmitChanges = { it1, it2, it3, it4, it5, it6 ->
                viewModel.onSubmitChanges(it1, it2, it3, it4, it5, it6)
                onNavigateBack()
            },
            onUpdateImage = { context, uri, game ->
                viewModel.saveUriToInternalStorage(context, uri, game)
            },
            onClearImage = {
                viewModel.clearCoverArt(it)
            }
        )
    }

}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailEditScreenBody(
    game: Game,
    gameGenres: List<Genre>,
    allGameGenres: List<Genre>,
    onNavigateBack: () -> Unit,
    onSubmitChanges: (String, String, String, Set<String>, Set<String>, List<Genre>) -> Unit,
    onUpdateImage: suspend (Context, Uri, Game) -> File? = { _, _, _ -> null },
    onClearImage: suspend (Game) -> Unit = {}
) {
    var titleState by remember { mutableStateOf(game.title) }
    var sortingNameState by remember { mutableStateOf(game.sortingName) }
    var descriptionState by remember { mutableStateOf(game.description) }
    var developerState by remember { mutableStateOf(game.developers.toList()) }
    var publisherState by remember { mutableStateOf(game.publishers.toList()) }

    var genreState by remember { mutableStateOf(gameGenres) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()


    // Registers a photo picker activity launcher in single-select mode.
    val pickMedia = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the
        // photo picker.
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            coroutineScope.launch {
                val savedFile = onUpdateImage(context, uri, game)
                if (savedFile != null) {
                    Log.i("PhotoPicker", "Saved successfully to: ${savedFile.absolutePath}")
                } else {
                    Log.e("PhotoPicker", "Failed to save image.")
                }
            }
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.height(48.dp)
                        )
                    }
                },
                windowInsets = WindowInsets(
                    top = 0.dp,
                    bottom = 0.dp
                ),
                // TODO have actions with hide/delete game
            )
        },
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface)
    )
    {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                // 1. The background sets the baseline height
                val coverHeight = LocalConfiguration.current.screenHeightDp.dp / 3.5f
                BackgroundArt(
                    game = game,
                    modifier = Modifier
                        .height(coverHeight)
                        .fillMaxWidth()
                        .blur(5.dp)
                )

                // 2. The Column sits inside the Box, aligned to the top center
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = (coverHeight / 2f))
                ) {
                    CoverArt(
                        game = game,
                        modifier = Modifier
                            .height(coverHeight)
                            .clip(RoundedCornerShape(15.dp)),
                        displayText = false,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row() {
                        AnnotatedButton(
                            onClick = {
                                pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                            },
                            iconPainter = painterResource(id = R.drawable.mic_image),
                            label = "Edit Cover",
                            colors = ButtonDefaults.filledTonalButtonColors()
                        )
                        FilledIconButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    onClearImage(game)
                                }
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(),
                            enabled = game.customCover != ""
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear custom cover"
                            )
                        }
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(10.dp, 10.dp, 10.dp, 0.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.mic_title),
                        contentDescription = "Title"
                    )
                    TextField(
                        value = titleState,
                        onValueChange = { titleState = it },
                        label = { Text("Title") },
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .widthIn(488.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.mic_sortingname),
                        contentDescription = "Sorting name"
                    )
                    TextField(
                        value = sortingNameState,
                        onValueChange = { sortingNameState = it },
                        label = { Text("Sorting Name") },
                        modifier = Modifier
                            .widthIn(488.dp)
                    )
                }
            }
            // TODO have an image of the background which you can change at some point
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(10.dp, 0.dp, 10.dp, 0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mic_info),
                    contentDescription = "Description"
                )
                TextField(
                    value = descriptionState,
                    onValueChange = { descriptionState = it },
                    label = { Text("Description") },
                    singleLine = false,
                    modifier = Modifier
                        .width(800.dp)
                        .widthIn(100.dp, 800.dp)

                )
            }

            // have a list of text fields for developer/publisher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(10.dp, 0.dp, 10.dp, 0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mic_developer),
                    contentDescription = "Developer icon"
                )
                SetItemCombo(
                    itemList = developerState,
                    label = "Developer",
                    onValueChange = { developerState = it })
            }

            // have a list of text fields for developer/publisher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(10.dp, 0.dp, 10.dp, 0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.mic_publisher),
                    contentDescription = "Publisher icon"
                )
                SetItemCombo(
                    itemList = publisherState,
                    label = "Publisher",
                    onValueChange = { publisherState = it })
            }

            // have chips for genre, with X on them and the ability to add more with a button
            GenreChips(
                genres = genreState,
                allGenres = allGameGenres,
                onValueChange = { genreState = it },
                modifier = Modifier.padding(10.dp, 0.dp, 10.dp, 0.dp)
            )

            // have a big "submit" button that saves changes and calls stuff to the database
            AnnotatedButton(
                onClick = {
                    onSubmitChanges(
                        titleState,
                        sortingNameState,
                        descriptionState,
                        developerState.toSet(),
                        publisherState.toSet(),
                        genreState
                    )
                },
                iconPainter = painterResource(R.drawable.mic_save),
                label = "Save"
            )
        }
    }
}


@Composable
private fun SetItemCombo(
    itemList: List<String>,
    label: String,
    onValueChange: (List<String>) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        itemList.forEachIndexed { index, item ->
            key(index) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = item,
                        onValueChange = {
                            var mutableList = itemList.toMutableList()
                            mutableList[index] = it
                            onValueChange(mutableList)
                        },
                        label = @Composable { Text(label) },
                        singleLine = true,
                        modifier = Modifier
                            .width(500.dp)
                            .widthIn(100.dp, 500.dp)
                    )
                    IconButton(
                        onClick = {
                            var mutableList = itemList.toMutableList()
                            mutableList.removeAt(index)
                            onValueChange(mutableList)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Delete ${label.lowercase()}"
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                onValueChange(itemList.plus(""))
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add ${label.lowercase()} icon"
                )
                Text("Add ${label.lowercase()}")
            }
        }

    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChips(
    genres: List<Genre>,
    allGenres: List<Genre>,
    onValueChange: (List<Genre>) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.mic_tag),
            contentDescription = "Genre"
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            genres.forEachIndexed { index, genre ->
                key(index) {
                    InputChip(
                        onClick = {
                            onValueChange(genres.minus(genre))
                        },
                        label = { Text(genre.name) },
                        selected = false,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear genre"
                            )
                        }
                    )
                }
            }
            var genreBoxState by remember { mutableStateOf("") }
            var genreSearchState by remember { mutableStateOf(listOf<Genre>()) }
            Column {
                genreSearchState.forEachIndexed { index, genre ->
                    key(index) {
                        InputChip(
                            onClick = {
                                if (!genres.contains(genre)) {
                                    onValueChange(genres.plus(genre))
                                    genreBoxState = ""
                                    genreSearchState = listOf()
                                }
                            },
                            label = { Text(genre.name) },
                            selected = true,
                            avatar = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add genre"
                                )
                            }
                        )
                    }
                }
            }

            TextField(
                value = genreBoxState,
                onValueChange = {
                    genreBoxState = it
                    genreSearchState = allGenres.filter { genre ->
                        genre.name.lowercase().startsWith(it.lowercase())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text("Add genre") }
            )
        }
    }
}

@Preview
@Composable
private fun DetailEditScreenPreview() {
    val game = Game(
        title = "Stardew Valley",
        description = "Stardew Valley is an open-ended country-life RPG! You’ve inherited your grandfather’s old farm plot in Stardew Valley. Armed with hand-me-down tools and a few coins, you set out to begin your new life. Can you learn to live off the land and turn these overgrown fields into a thriving home? It won’t be easy. Ever since Joja Corporation came to town, the old ways of life have all but disappeared. The community center, once the town’s most vibrant hub of activity, now lies in shambles. But the valley seems full of opportunity. With a little dedication, you might just be the one to restore Stardew Valley to greatness!",
        sortingName = "Stardew Valley",
        status = PlayStatus.Beaten,
        imageUrl = "",
        backgroundUrl = "",
        playTime = 22255,
        steamId = 2L,
        epicId = "a",
        developers = setOf("ConcernedApe", "Support Studio"),
        publishers = setOf("Chucklefish", "ConcernedApe"),
        genre = setOf()
    )
    val gameGenres = listOf(
        Genre("Indie", 0, 0),
        Genre("RPG", 0, 0),
        Genre("Simulation", 0, 0)
    )
    DetailEditScreenBody(
        game, gameGenres, listOf(), {}, { it1, it2, it3, it4, it5, it6 -> }
    )
}

@Preview(
    widthDp = 300,
    heightDp = 500
)
@Composable
private fun DetailEditScreenPreviewDiffScreen() {
    val game = Game(
        title = "Stardew Valley",
        description = "Stardew Valley is an open-ended country-life RPG! You’ve inherited your grandfather’s old farm plot in Stardew Valley. Armed with hand-me-down tools and a few coins, you set out to begin your new life. Can you learn to live off the land and turn these overgrown fields into a thriving home? It won’t be easy. Ever since Joja Corporation came to town, the old ways of life have all but disappeared. The community center, once the town’s most vibrant hub of activity, now lies in shambles. But the valley seems full of opportunity. With a little dedication, you might just be the one to restore Stardew Valley to greatness!",
        sortingName = "Stardew Valley",
        status = PlayStatus.Beaten,
        imageUrl = "",
        backgroundUrl = "",
        playTime = 22255,
        steamId = 2L,
        epicId = "a",
        developers = setOf("ConcernedApe", "Support Studio"),
        publishers = setOf("Chucklefish", "ConcernedApe"),
        genre = setOf()
    )
    val gameGenres = listOf(
        Genre("Indie", 0, 0),
        Genre("RPG", 0, 0),
        Genre("Simulation", 0, 0)
    )
    DetailEditScreenBody(
        game, gameGenres, listOf(), {}, { it1, it2, it3, it4, it5, it6 -> }
    )
}

@Preview(showBackground = true)
@Composable
private fun GenreChipsPreview() {
    GenreChips(
        genres = listOf(Genre("what"), Genre("how"), Genre("when")),
        allGenres = listOf(Genre("what"), Genre("how"), Genre("when")),
        onValueChange = {},
        modifier = Modifier
    )
}