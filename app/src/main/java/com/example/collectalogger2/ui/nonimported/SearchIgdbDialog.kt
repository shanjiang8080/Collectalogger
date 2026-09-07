package com.example.collectalogger2.ui.nonimported

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.ui.shared.CoverArt

/**
 * Dialog for matching selected non-imported games to IGDB games.
 * For each selected game, a dropdown shows the IGDB search results for its name.
 * The user picks one of these to import the game and take the metadata from.
 */
@Composable
fun SearchIgdbDialog(
    uiState: NonImportedUiState,
    onChooseResult: (key: String, igdbGame: Game) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // The selection keys with an open dropdown
    var expandedKeys by remember { mutableStateOf(emptySet<String>()) }

    val selectedGames = uiState.gamesByLibrary.flatMap { (library, games) ->
        games.filter { selectionKey(library, it) in uiState.selectedKeys }
            .map { library to it }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Search IGDB",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(selectedGames) { _, selectedGame ->
                        val (library, game) = selectedGame
                        val key = selectionKey(library, game)
                        SearchResultDropdown(
                            game = game,
                            results = uiState.searchResults[key],
                            chosen = uiState.chosenResults[key],
                            expanded = expandedKeys.contains(key),
                            onToggleExpanded = {
                                expandedKeys = if (expandedKeys.contains(key)) {
                                    expandedKeys - key
                                } else {
                                    expandedKeys + key
                                }
                            },
                            onSelectResult = { igdbGame ->
                                onChooseResult(key, igdbGame)
                                expandedKeys = expandedKeys - key
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = uiState.chosenResults.isNotEmpty()
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

/**
 * The dropdown for one selected game, with the chosen IGDB result displayed
 * below it (cover art, title, publisher) once one is picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultDropdown(
    game: Game,
    results: List<Game>?, // null while the IGDB search is still running
    chosen: Game?,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSelectResult: (Game) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { onToggleExpanded() }
        ) {
            TextField(
                readOnly = true,
                value = if (chosen != null) chosen.title
                else game.title.ifEmpty { "(Unknown title)" },
                onValueChange = {},
                label = { Text(game.title.ifEmpty { "(Unknown title)" }) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = onToggleExpanded
            ) {
                when {
                    results == null -> {
                        DropdownMenuItem(
                            text = { Text("Searching IGDB...") },
                            onClick = {},
                            enabled = false
                        )
                    }
                    results.isEmpty() -> {
                        DropdownMenuItem(
                            text = { Text("No IGDB results found") },
                            onClick = {},
                            enabled = false
                        )
                    }
                    else -> results.forEach { result ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = result.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = result.publishers.joinToString(", ")
                                            .ifEmpty { "No publisher listed" },
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            leadingIcon = {
                                CoverArt(
                                    game = result,
                                    displayText = false,
                                    modifier = Modifier.width(32.dp)
                                )
                            },
                            onClick = { onSelectResult(result) }
                        )
                    }
                }
            }
        }
        if (chosen != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(
                    game = chosen,
                    displayText = true,
                    modifier = Modifier.width(72.dp)
                )
                Column {
                    Text(
                        text = chosen.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = chosen.publishers.joinToString(", ")
                            .ifEmpty { "No publisher listed" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
