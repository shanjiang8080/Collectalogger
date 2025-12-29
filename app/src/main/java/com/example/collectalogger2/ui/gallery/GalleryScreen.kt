package com.example.collectalogger2.ui.gallery

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.collectalogger2.R
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.Genre
import com.example.collectalogger2.ui.gallery.DialogActionType.CheckNonImportedItems
import com.example.collectalogger2.ui.overlays.EpicOverlay
import com.example.collectalogger2.ui.overlays.SteamOverlay
import com.example.collectalogger2.util.Filter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onNavigateToDetail: (id: Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val textFieldState = rememberTextFieldState(initialText = "")
    val listState = rememberSaveable(saver = LazyGridState.Saver) {
        LazyGridState()
    }
    val allGenres by viewModel.allGameGenres.collectAsStateWithLifecycle()
    val loadPercentage by viewModel.loadPercentage.collectAsStateWithLifecycle()
    // eventually, the uiState can be changed by filters.
    // not now, though.
    GalleryScreenBody(
        updateGames = { viewModel.updateGames() },
        textFieldState = textFieldState,
        listState = listState,
        uiState = uiState,
        allGenres = allGenres,
        onUpdateFilter = { viewModel.updateFilter(it) },
        onNavigateToDetail = onNavigateToDetail,
        getSearchedGamesList = { it -> viewModel.getSearchedGamesList(it) },
        onSearch = { it -> viewModel.getSearchedGames(it) },
        uiEvents = viewModel.uiEvents,
        saveSteamId = { it -> viewModel.saveSteamId(it) },
        saveEpicId = { it -> viewModel.saveEpicInfo(it) },
        loadPercentage = loadPercentage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GalleryScreenBody(
    updateGames: () -> Unit,
    listState: LazyGridState,
    textFieldState: TextFieldState,
    uiState: GalleryUiState,
    onUpdateFilter: (Filter) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    getSearchedGamesList: (String) -> List<Game>,
    onSearch: (String) -> Unit,
    allGenres: List<Genre>,
    uiEvents: SharedFlow<UiEvent>,
    saveSteamId: (String) -> Unit,
    saveEpicId: (String) -> Unit,
    loadPercentage: Float,
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    // val sheetState = rememberModalBottomSheetState()
    // val scope = rememberCoroutineScope()

    var favoritesChecked by remember { mutableStateOf(uiState.filter?.isFavorite == true) }
    var sortMenuBoxState by remember { mutableStateOf(false) }
    var genreMenuBoxState by remember { mutableStateOf(false) }
    var selectedGenreState by remember { mutableStateOf<Genre?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var dialogState by remember { mutableStateOf<DialogActionType?>(null) }
    // To handle loading, and also disable the refresh button
    var loading by remember { mutableStateOf(false) }

    // Dunno where to put this
    LaunchedEffect(Unit) {
        uiEvents.collect { event ->
            when (event) {
                is UiEvent.ShowDialog -> {
                    dialogState = event.actionType
                }
                is UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }
                is UiEvent.ShowSnackbar -> {
                    val actionLabel: String?
                    val withDismissAction: Boolean
                    val duration: SnackbarDuration
                    val performedAction: () -> Unit
                    when (event.actionType) {
                        SnackbarActionType.Info -> {
                            actionLabel = null
                            withDismissAction = false
                            duration = SnackbarDuration.Short
                        }
                        is SnackbarActionType.NonImportedGames -> {
                            actionLabel = "Review"
                            withDismissAction = true
                            duration = SnackbarDuration.Indefinite
                            performedAction = {
                                dialogState = CheckNonImportedItems(event.actionType.items)
                            }
                        }
                    }

                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = actionLabel,
                        withDismissAction = withDismissAction,
                        duration = duration
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        performedAction()
                    }

                }
                is UiEvent.LoadingFinished -> {
                    loading = false
                }
            }
        }

    }

    if (dialogState != null) {
        when (dialogState!!) {
            is CheckNonImportedItems -> {
                // Nothing for now
                // TODO create a composable to try to import them or ignore them
                (dialogState as CheckNonImportedItems).items.forEach {
                    it.value.forEach {
                        Log.i("GalleryScreen", "Not imported: ${it.title}")
                    }
                }
            }
            is DialogActionType.LoggedOut -> {
                // onDismiss is the same always, so define it here
                var onDismiss = { dialogState = null }
                // UPDATE WHEN ADDING LIBRARIES
                when ((dialogState as DialogActionType.LoggedOut).library) {
                    "Steam" -> {
                        SteamOverlay(onDismiss, saveSteamId)
                    }
                    "Epic Games" -> {
                        EpicOverlay(onDismiss, saveEpicId)
                    }
                }
            }
        }
    }


    Scaffold(
        topBar = {
            if (loading && loadPercentage != -1f) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    progress = { loadPercentage },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            FloatingRefreshButton({
                updateGames(); loading = true; Log.d(
                "GalleryScreen",
                "Loading button pressed!"
            )
            }, loading)
        }
    ) {
        Column {
            FilterSearchBar(
                textFieldState = textFieldState,
                onSearch = onSearch,
                getSearchedGamesList = getSearchedGamesList,
                onNavigateToDetail = onNavigateToDetail,
                onClickFilterButton = { showFilterSheet = true },
                // add more when more filters get added
                hasFiltersApplied = favoritesChecked
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                state = listState,
            ) {
                if (uiState.filter != null) {
                    items(uiState.filter.getFilteredItems(uiState.games)) {
                        GalleryGame(it, onNavigateToDetail)
                    }
                } else {
                    items(uiState.games) {
                        GalleryGame(it, onNavigateToDetail)
                    }
                }
            }

        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false }
            ) {
                Column {
                    // have a row for sorts...
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(15.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Sort by",
                        )
                        ExposedDropdownMenuBox(
                            expanded = sortMenuBoxState,
                            onExpandedChange = { sortMenuBoxState = it },
                            modifier = Modifier,
                        ) {
                            // nothing yet TODO implement sort by
                        }
                    }

                    // have a row for filters by favorites
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(15.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Favorites only",
                        )
                        Switch(
                            checked = favoritesChecked,
                            onCheckedChange = {
                                favoritesChecked = it
                                onUpdateFilter(uiState.filter!!.copy(isFavorite = it))
                            }
                        )
                    }
                    // have a row for filters by genre
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(15.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Sort by",
                        )
                        ExposedDropdownMenuBox(
                            expanded = genreMenuBoxState,
                            onExpandedChange = { genreMenuBoxState = !genreMenuBoxState },
                        ) {
                            TextField(
                                readOnly = true,
                                value = selectedGenreState?.name ?: "Select genre",
                                onValueChange = { },
                                label = { Text("Genre") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = genreMenuBoxState
                                    )
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier.menuAnchor(
                                    MenuAnchorType.PrimaryNotEditable,
                                    true
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = genreMenuBoxState,
                                onDismissRequest = { genreMenuBoxState = false },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .width(200.dp) // probably use the device screen size TODO
                                        .height(300.dp)
                                ) {
                                    LazyColumn {
                                        item {
                                            DropdownMenuItem(
                                                text = { Text("No filters") },
                                                onClick = {
                                                    selectedGenreState = null
                                                    genreMenuBoxState = false
                                                    onUpdateFilter(uiState.filter!!.copy(genre = null))
                                                }
                                            )
                                        }
                                        items(count = allGenres.size) { num ->
                                            val genre = allGenres[num]
                                            DropdownMenuItem(
                                                text = { Text(genre.name) },
                                                onClick = {
                                                    selectedGenreState = genre
                                                    genreMenuBoxState = false
                                                    onUpdateFilter(
                                                        uiState.filter!!.copy(
                                                            genre = listOf(
                                                                genre.igdbId
                                                            )
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // have a row for filters by library
                }
            }
        }
    }
}


@Composable
fun FilterRow(
    name: String,
    filterComposable: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(15.dp)
    ) {
        Text(
            text = name,
        )
        filterComposable
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    getSearchedGamesList: (String) -> List<Game>,
    onNavigateToDetail: (Long) -> Unit,
    onClickFilterButton: () -> Unit,
    hasFiltersApplied: Boolean = false
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchResults = getSearchedGamesList(textFieldState.text.toString())

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = textFieldState.text.toString(),
                onQueryChange = { textFieldState.edit { replace(0, length, it) } },
                onSearch = {
                    onSearch(textFieldState.text.toString())
                    expanded = false
                },
                expanded = expanded,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search button")
                },
                trailingIcon = {
                    Row {
                        if (textFieldState.text.isNotEmpty()) {
                            SearchClearButton(clearFilter = {
                                onSearch("")
                                textFieldState.edit { replace(0, length, "") } // Clears the text
                            })
                        }
                        FilterButton(
                            hasFiltersApplied,
                            onClickFilterButton
                        ) // TODO implement this later
                    }
                },
                onExpandedChange = { expanded = it },
                placeholder = { Text("Search your library") }
            )
        },
        expanded = expanded,
        onExpandedChange = {it: Boolean -> expanded = it},
        windowInsets = WindowInsets(top = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp, 7.dp, 10.dp, 10.dp)
    ) {
        LazyColumn {
            items(count = searchResults.size) { index ->
                val resultGame = searchResults[index]
                val resultText = resultGame.title
                ListItem(
                    headlineContent = { Text(resultText) },
                    leadingContent = {
                        GalleryGame(resultGame, {}) // no onClick, since it's given to the parent
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .clickable {
                            onNavigateToDetail(resultGame.id)
                            expanded = false
                        }
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(100.dp)
                )

            }
        }
    }

}
@Composable
fun FilterButton(
    hasFilters: Boolean,
    openFilter: () -> Unit
) {
    IconButton(onClick = openFilter) {
        if (hasFilters) {
            Icon(
                painter = painterResource(id = R.drawable.mic_filter_filled),
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = "Favorite button (filled)"
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.mic_filter_unfilled),
                tint = MaterialTheme.colorScheme.onSurface,
                contentDescription = "Favorite button (unfilled)"
            )
        }
    }

}
@Composable
fun SearchClearButton(clearFilter: () -> Unit) {
    IconButton(onClick = clearFilter) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Clear search query",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun FloatingRefreshButton(onClick: () -> Unit, loading: Boolean) {
    if (!loading) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.secondary,
        ) {
            Icon(Icons.Filled.Refresh, "Update games button.")
        }
    } else {
        FloatingActionButton(
            onClick = {},
            containerColor = MaterialTheme.colorScheme.surfaceDim,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Icon(Icons.Filled.Refresh, "Update games button (disabled).")
        }
    }
}

@Composable
fun GalleryGame(
    game: Game,
    onNavigateToDetail: (id: Long) -> Unit,
) {
    val gameModifier = Modifier
        .clickable(onClick = { onNavigateToDetail(game.id) })
    if (game.imageUrl != "") {
        AsyncImage(
            model = game.imageUrl,
            contentDescription = game.title,
            // add modifier = Modifier.clickable
            // to make it work!
            modifier = gameModifier.aspectRatio(0.75f),

        )
    } else {
        Box(
            modifier = gameModifier,
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.not_found),
                contentDescription = game.title,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.aspectRatio(0.75f)
            )
            Text(text = game.title, color = Black)
        }
    }
}

@Preview(widthDp = 400,heightDp = 800)
@Composable
fun GalleryPreview() {
    var uiState = GalleryUiState(games = listOf(
        Game("Rogue Legacy II"),
        Game("Stardew Valley"),
        Game("Celeste"),
        Game("Super Meat Boy")
    ))

    GalleryScreenBody(
        updateGames = {},
        listState = LazyGridState(),
        textFieldState = TextFieldState(),
        uiState = uiState,
        onUpdateFilter = {},
        onNavigateToDetail = {},
        getSearchedGamesList = { return@GalleryScreenBody listOf<Game>() },
        onSearch = {},
        allGenres = listOf(),
        uiEvents = MutableSharedFlow<UiEvent>(),
        saveSteamId = {},
        saveEpicId = {},
        loadPercentage = -1f
    )
}
@Preview(
    showBackground = true
)
@Composable
fun GalleryGameNoImagePreview() {
    val game = Game("Rogue Legacy II", 19)
    GalleryGame(game, {})
}

