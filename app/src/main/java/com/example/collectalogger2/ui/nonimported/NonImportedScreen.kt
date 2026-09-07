package com.example.collectalogger2.ui.nonimported

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.collectalogger2.data.Game

// Screen listing the games that failed to automatically import,
// grouped by the library they came from.
// The user can select results and ignore them (so they won't be imported
// or reported in the future), or manually match them to IGDB games
// with the Search IGDB dialog.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NonImportedScreen(
    viewModel: NonImportedViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Non-Imported Games")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.gamesByLibrary.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No non-imported games")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Action buttons, sticking to the top of the list while scrolling
                stickyHeader(key = "actions") {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.ignoreSelected() },
                                enabled = uiState.selectedKeys.isNotEmpty()
                            ) {
                                Text("Ignore")
                            }
                            Button(
                                onClick = { viewModel.openSearchDialog() },
                                enabled = uiState.selectedKeys.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search IGDB")
                            }
                        }
                    }
                }
                uiState.gamesByLibrary.forEach { (library, games) ->
                    item(key = "header_$library") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = library,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${games.size} not imported",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(games) { game ->
                        val key = selectionKey(library, game)
                        NonImportedGameRow(
                            game = game,
                            isSelected = uiState.selectedKeys.contains(key),
                            onToggleSelection = { viewModel.toggleSelection(key) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showSearchDialog) {
        SearchIgdbDialog(
            uiState = uiState,
            onChooseResult = { key, igdbGame -> viewModel.chooseIgdbResult(key, igdbGame) },
            onConfirm = { viewModel.confirmSearchDialog() },
            onDismiss = { viewModel.dismissSearchDialog() }
        )
    }
}

@Composable
fun NonImportedGameRow(
    game: Game,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleSelection),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection() }
        )
        Text(
            text = game.title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
