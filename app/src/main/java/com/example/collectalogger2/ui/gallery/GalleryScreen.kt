package com.example.collectalogger2.ui.gallery

import android.annotation.SuppressLint
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
    // eventually, the uiState can be changed by filters.
    // not now, though.
    GalleryScreenBody(
        updateGames = { viewModel.updateGames() },
        textFieldState = textFieldState,
        listState = listState,
        uiState = uiState,
        onNavigateToDetail = onNavigateToDetail,
        getSearchedGamesList = {it -> viewModel.getSearchedGamesList(it) },
        onSearch = {it -> viewModel.getSearchedGames(it) }
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun GalleryScreenBody(
    updateGames: () -> Unit,
    listState: LazyGridState,
    textFieldState: TextFieldState,
    uiState: GalleryUiState,
    onNavigateToDetail: (Long) -> Unit,
    getSearchedGamesList: (String) -> List<Game>,
    onSearch: (String) -> Unit
) {
    Scaffold(
        topBar = {

        },
        floatingActionButton = {
            FloatingRefreshButton(updateGames)
        }
    ) {
        Column {
            FilterSearchBar(
                textFieldState = textFieldState,
                onSearch = onSearch,
                getSearchedGamesList = getSearchedGamesList,
                onNavigateToDetail = onNavigateToDetail
            )
            // eventually, make the size changeable via a setting!
            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                state = listState,
            ) {
                items(uiState.games) {
                    GalleryGame(it, onNavigateToDetail)
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
    onNavigateToDetail: (Long) -> Unit
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
                        FilterButton(false, {}) // TODO implement this later
                    }
                },
                onExpandedChange = { expanded = it },
                placeholder = { Text("Search your library") }
            )
        },
        expanded = expanded,
        onExpandedChange = {it: Boolean -> expanded = it},
        windowInsets = WindowInsets(bottom = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
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
fun FloatingRefreshButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.secondary
    ) {
        Icon(Icons.Filled.Refresh, "Update games button.")
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
        onNavigateToDetail = {},
        onSearch = {},
        getSearchedGamesList = { return@GalleryScreenBody listOf<Game>() },
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

@Preview
@Composable
fun FilterSearchBarPreview() {
    FilterSearchBar(
        textFieldState = TextFieldState(),
        onSearch = { },
        getSearchedGamesList = {return@FilterSearchBar emptyList()},
        onNavigateToDetail = {},
    )
}

