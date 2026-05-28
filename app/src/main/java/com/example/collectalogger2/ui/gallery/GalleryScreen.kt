package com.example.collectalogger2.ui.gallery

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
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
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.collectalogger2.R
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.Genre
import com.example.collectalogger2.ui.gallery.DialogActionType.CheckNonImportedItems
import com.example.collectalogger2.ui.overlays.EpicOverlay
import com.example.collectalogger2.ui.overlays.SteamOverlay
import com.example.collectalogger2.ui.shared.CoverArt
import com.example.collectalogger2.util.Filter
import com.example.collectalogger2.util.LocalNavEventBus
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

    // Go back to the top
    val navBus = LocalNavEventBus.current

    LaunchedEffect(navBus) {
        navBus.scrollToTopEvent.collect { route ->
            if (route == "Gallery") {
                // 3. Perform the action
                listState.animateScrollToItem(0)
            }
        }
    }

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
        loadPercentage = loadPercentage,
        toggleSelection = { viewModel.toggleSelection(it) },
        clearSelection = { viewModel.clearSelection() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    toggleSelection: (Long) -> Unit,
    clearSelection: () -> Unit = {}
) {
    // Filter/sort states
    var showFilterSheet by remember { mutableStateOf(false) }

    var favoritesChecked by remember { mutableStateOf(uiState.filter?.isFavorite == true) }
    var sortMenuBoxState by remember { mutableStateOf(false) }
    var genreMenuBoxState by remember { mutableStateOf(false) }
    var selectedGenreState by remember { mutableStateOf<Genre?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // UI states
    var dialogState by remember { mutableStateOf<DialogActionType?>(null) }
    val topBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Bulk selection states
    val isSelectionMode = uiState.selectedIds.isNotEmpty()


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
            if (isSelectionMode) { // TODO have an else with AppBarWithSearch
                TopAppBar(
                    title = {
                        Text("${uiState.selectedIds.size} selected")
                    },
                    modifier = Modifier,
                    navigationIcon = {
                        IconButton(onClick = clearSelection) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Selection"
                            )
                        }
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                    scrollBehavior = topBarScrollBehavior,
                )
            }
            if (loadPercentage != -1f) {
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
            FloatingRefreshButton({ updateGames() }, loadPercentage != -1f)
        },
        contentWindowInsets = WindowInsets.statusBars
    ) {

        var searchBarHeightDp by remember { mutableStateOf(0.dp) }
        val density = LocalDensity.current

        Box(
            contentAlignment = Alignment.TopCenter
        ) {
            // TODO wrap this in an if statement, hopefully with an animation tween offscreen


            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                state = listState,
                contentPadding = PaddingValues(top = searchBarHeightDp + 16.dp)
            ) {
                if (uiState.filter != null) {
                    items(uiState.filter.getFilteredItems(uiState.games)) {
                        GalleryGame(
                            game = it,
                            onNavigateToDetail = onNavigateToDetail,
                            isSelectionMode = isSelectionMode,
                            isSelected = it.id in uiState.selectedIds,
                            toggleSelection = toggleSelection
                        )
                    }
                } else {
                    items(uiState.games) {
                        GalleryGame(
                            game = it,
                            onNavigateToDetail = onNavigateToDetail,
                            isSelectionMode = isSelectionMode,
                            isSelected = it.id in uiState.selectedIds,
                            toggleSelection = toggleSelection
                        )
                    }
                }
            }
            FilterSearchBar(
                textFieldState = textFieldState,
                onSearch = onSearch,
                getSearchedGamesList = getSearchedGamesList,
                onNavigateToDetail = onNavigateToDetail,
                onClickFilterButton = { showFilterSheet = true },
                // add more when more filters get added
                hasFiltersApplied = favoritesChecked,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        // Measure the height in pixels and convert to Dp
                        if (searchBarHeightDp == 0.dp) {
                            searchBarHeightDp = with(density) { coordinates.size.height.toDp() }
                        }
                    }
            )
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
                            .padding(16.dp)
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
                            .padding(16.dp)
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
                            .padding(16.dp)
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    getSearchedGamesList: (String) -> List<Game>,
    onNavigateToDetail: (Long) -> Unit,
    onClickFilterButton: () -> Unit,
    modifier: Modifier = Modifier,
    hasFiltersApplied: Boolean = false,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchResults = getSearchedGamesList(textFieldState.text.toString())

    DockedSearchBar(
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
        modifier = modifier
    ) {
        LazyColumn {
            items(count = searchResults.size) { index ->
                val resultGame = searchResults[index]
                val resultText = resultGame.title
                ListItem(
                    headlineContent = { Text(resultText) },
                    leadingContent = {
                        CoverArt(
                            game = resultGame,
                            displayText = false,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        ) // no onClick, since it's given to the parent
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryGame(
    game: Game,
    onNavigateToDetail: (id: Long) -> Unit,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    toggleSelection: (id: Long) -> Unit = {},
) {
    val animationSpec = tween<Float>(durationMillis = 100, easing = EaseOutBack)
    val dpAnimationSpec = tween<Dp>(durationMillis = 100, easing = EaseOut)

    // 1. Cover Art Scale Animation
    val contentScale by animateFloatAsState(
        targetValue = if (isSelected) 0.8f else 1.0f,
        animationSpec = animationSpec,
        label = "CoverArtScale"
    )

    // 2. Corner Radius Animation (e.g., from 8.dp normal to 24.dp when selected)
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 8.dp,
        animationSpec = dpAnimationSpec,
        label = "CornerRadius"
    )

    // 3. Checkbox Scale Animation (0.0f to 1.0f)
    val checkboxScale by animateFloatAsState(
        targetValue = if (isSelectionMode) 1.0f else 0.0f,
        animationSpec = animationSpec,
        label = "CheckboxScale"
    )

    Box(
        modifier = Modifier

            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        toggleSelection(game.id)
                    } else {
                        onNavigateToDetail(game.id)
                    }
                },
                onLongClick = {
                    toggleSelection(game.id)
                }
            )
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Cover Art Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSelectionMode) MaterialTheme.colorScheme.secondaryContainer
                    else Color.Transparent
                )
                .scale(contentScale)
                // Dynamically clips the art using the animated radius
                .clip(RoundedCornerShape(cornerRadius)),
            contentAlignment = Alignment.Center
        ) {
            CoverArt(game = game)
        }

        // Checkbox Overlay
        // The Box always occupies its 24.dp spot in the TopEnd, preventing position shifting
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                // We scale the layout from its absolute center
                .scale(checkboxScale)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.White.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected && checkboxScale > 0.5f) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(widthDp = 400,heightDp = 800)
@Composable
private fun GalleryPreview() {
    var uiState = GalleryUiState(games = listOf(
        Game("Rogue Legacy II"),
        Game("Stardew Valley"),
        Game("Celeste"),
        Game("Super Meat Boy"),
        Game("Yeah! You Want \"Those Games\", Right? So Here You Go! Now, Let's See You Clear Them!\n")
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
        loadPercentage = -1f,
        toggleSelection = {}
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Preview
@Composable
private fun GalleryGameSelectedPreview() {
    val games = listOf(
        Game("Rogue Legacy II"),
        Game("Stardew Valley"),
        Game("Celeste"),
        Game("Super Meat Boy"),
        Game("Yeah! You Want \"Those Games\", Right? So Here You Go! Now, Let's See You Clear Them!\n")
    )

    FlowRow {
        for (game in games) {
            GalleryGame(
                game = game,
                onNavigateToDetail = {},
                isSelectionMode = true,
                isSelected = true,
                toggleSelection = {},
            )
        }
    }
}