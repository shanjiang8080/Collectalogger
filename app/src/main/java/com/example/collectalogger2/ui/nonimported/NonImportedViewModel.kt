package com.example.collectalogger2.ui.nonimported

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.ImportIgnoredResult
import com.example.collectalogger2.data.repository.GameLibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NonImportedUiState(
    // Games that failed to import, grouped by the library they came from
    val gamesByLibrary: Map<String, List<Game>> = emptyMap(),
    val selectedKeys: Set<String> = emptySet(),
    // Search IGDB dialog state
    val showSearchDialog: Boolean = false,
    // Partial IGDB games per selection key; an entry is absent while its search is running
    val searchResults: Map<String, List<Game>> = emptyMap(),
    // The IGDB game chosen for each selection key
    val chosenResults: Map<String, Game> = emptyMap()
)

// Keys are "<library name>:<game title>", disambiguated by the game's
// store-specific ID, so results with the same name stay independent
fun selectionKey(library: String, game: Game): String {
    val storeIds = buildList {
        if (game.steamId != -1L) add(game.steamId.toString())
        if (game.epicId.isNotEmpty()) add(game.epicId)
        if (game.gogId.isNotEmpty()) add(game.gogId)
        if (game.itchId.isNotEmpty()) add(game.itchId)
        if (game.amazonId.isNotEmpty()) add(game.amazonId)
    }.joinToString(separator = "#")
    return "$library:${game.title}:$storeIds"
}

class NonImportedViewModel(
    private val gameLibraryRepository: GameLibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NonImportedUiState())
    val uiState: StateFlow<NonImportedUiState> = _uiState.asStateFlow()

    // Results the user already resolved (imported manually), removed from the screen
    private val _resolvedKeys = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            combine(
                gameLibraryRepository.missingGames,
                gameLibraryRepository.ignoredResults,
                _resolvedKeys
            ) { missingGames, ignoredResults, resolvedKeys ->
                filterIgnored(missingGames, ignoredResults)
                    .mapValues { (library, games) ->
                        games.filterNot { game -> selectionKey(library, game) in resolvedKeys }
                    }
                    .filterValues { it.isNotEmpty() }
            }.collect { groupedGames ->
                _uiState.update { it.copy(gamesByLibrary = groupedGames) }
            }
        }
    }

    // Removes the results the user chose to ignore (in the past or just now)
    private fun filterIgnored(
        missingGames: Map<String, List<Game>>,
        ignoredResults: List<ImportIgnoredResult>
    ): Map<String, List<Game>> {
        return missingGames.mapValues { (library, games) ->
            games.filterNot { game ->
                ignoredResults.any { ignored ->
                    ignored.library == library && ignored.name.equals(game.title, ignoreCase = true)
                }
            }
        }.filterValues { it.isNotEmpty() }
    }

    // The currently selected games, with the library each one came from
    private fun getSelectedGames(): List<Pair<String, Game>> =
        _uiState.value.gamesByLibrary.flatMap { (library, games) ->
            games.filter { selectionKey(library, it) in _uiState.value.selectedKeys }
                .map { library to it }
        }

    fun toggleSelection(key: String) {
        _uiState.update {
            if (it.selectedKeys.contains(key)) {
                it.copy(selectedKeys = it.selectedKeys - key)
            } else {
                it.copy(selectedKeys = it.selectedKeys + key)
            }
        }
    }

    // Persists the selected results as ignored; the filtered combine flow
    // removes them from the screen once they are saved
    fun ignoreSelected() {
        viewModelScope.launch {
            val selectedKeys = _uiState.value.selectedKeys
            val selectedResults = mutableListOf<ImportIgnoredResult>()
            _uiState.value.gamesByLibrary.forEach { (library, games) ->
                games.forEach { game ->
                    if (selectionKey(library, game) in selectedKeys) {
                        selectedResults.add(ImportIgnoredResult(name = game.title, library = library))
                    }
                }
            }
            selectedResults.forEach { gameLibraryRepository.insertIgnoredResult(it) }
            _uiState.update { it.copy(selectedKeys = emptySet()) }
        }
    }

    // Opens the Search IGDB dialog and starts a search for each selected game
    fun openSearchDialog() {
        val selectedGames = getSelectedGames()
        if (selectedGames.isEmpty()) return
        _uiState.update {
            it.copy(showSearchDialog = true, searchResults = emptyMap(), chosenResults = emptyMap())
        }
        selectedGames.forEach { (library, game) ->
            viewModelScope.launch {
                val results = try {
                    gameLibraryRepository.searchIgdbGames(game.title)
                } catch (e: Exception) {
                    Log.w(
                        "NonImportedViewModel",
                        "IGDB search failed for \"${game.title}\": ${e.message}"
                    )
                    emptyList()
                }
                _uiState.update {
                    it.copy(searchResults = it.searchResults + (selectionKey(library, game) to results))
                }
            }
        }
    }

    fun chooseIgdbResult(key: String, igdbGame: Game) {
        _uiState.update { it.copy(chosenResults = it.chosenResults + (key to igdbGame)) }
    }

    fun dismissSearchDialog() {
        _uiState.update {
            it.copy(showSearchDialog = false, searchResults = emptyMap(), chosenResults = emptyMap())
        }
    }

    // Imports the selected games the user chose IGDB results for;
    // they disappear from the screen through the resolved results filter.
    // Runs on IO, since the repository does blocking database queries
    fun confirmSearchDialog() {
        viewModelScope.launch(Dispatchers.IO) {
            val importedKeys = mutableSetOf<String>()
            getSelectedGames().forEach { (library, game) ->
                val key = selectionKey(library, game)
                val igdbGame = _uiState.value.chosenResults[key] ?: return@forEach
                gameLibraryRepository.importNonImportedGame(library, game, igdbGame)
                importedKeys.add(key)
            }
            _resolvedKeys.update { it + importedKeys }
            _uiState.update { it.copy(selectedKeys = emptySet()) }
            dismissSearchDialog()
        }
    }
}

class NonImportedViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return NonImportedViewModel(container.gameLibraryRepository) as T
    }
}
