package com.example.collectalogger2.ui.detail

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.collectalogger2.AppContainer
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.Genre
import com.example.collectalogger2.data.repository.GameLibraryRepository
import com.example.collectalogger2.util.getExtensionFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DetailViewModel(
    val gameLibraryRepository: GameLibraryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _game = MutableStateFlow<Game?>(null)
    private val _currentDialog = MutableStateFlow<String>("")
    val game = _game.asStateFlow()
    private val _gameGenres = MutableStateFlow<List<Genre>>(emptyList())
    val gameGenres = _gameGenres.asStateFlow()

    private val _allGameGenres = MutableStateFlow<List<Genre>>(emptyList())
    val allGameGenres = _allGameGenres.asStateFlow()
    val currentDialog = _currentDialog.asStateFlow()
    val gameId: Long = checkNotNull(savedStateHandle["id"])
    init {
        viewModelScope.launch(Dispatchers.IO) {
            gameLibraryRepository.getGameStream(gameId).collect { flowGame: Game? ->
                if (flowGame != null) {
                    _game.value = flowGame
                    var newGenres = mutableListOf<Genre>()
                    flowGame.genre.forEach { genreId ->
                        var genre = gameLibraryRepository.getGenreByIGDBId(genreId)
                        if (genre != null) newGenres.add(genre)
                    }
                    _gameGenres.value = newGenres.toList()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            gameLibraryRepository.genreFlow.collect { genres ->
                _allGameGenres.value = genres
            }
        }
    }


    fun setDialog(newValue: String) {
        _currentDialog.value = newValue
    }

    fun editPlayStatus(newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            gameLibraryRepository.updateGame(_game.value!!.copy(status = newStatus))
        }
    }

    fun toggleFavoriteGame() {
        viewModelScope.launch(Dispatchers.IO) {
            gameLibraryRepository.updateGame(_game.value!!.copy(isFavorite = !_game.value!!.isFavorite))
            Log.i("DetailViewModel", "${_game.value!!.title} is now ${if (_game.value!!.isFavorite) "" else "un"}favorited.")
        }
    }

    fun onSubmitChanges(
        title: String,
        sortingName: String,
        description: String,
        developers: Set<String>,
        publishers: Set<String>,
        genres: List<Genre>
    ) {
        val newGenres = mutableListOf<Int>()
        genres.forEach { genre ->
            newGenres.add(genre.igdbId)
        }

        viewModelScope.launch(Dispatchers.IO) {
            gameLibraryRepository.updateGame(
                _game.value!!.copy(
                    title = title,
                    sortingName = sortingName,
                    description = description,
                    // for developers/publishers, remove empty strings
                    developers = developers.minus(""),
                    publishers = publishers.minus(""),
                    genre = newGenres.toSet()
                )
            )
        }
    }

    suspend fun saveUriToInternalStorage(context: Context, uri: Uri, game: Game): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Remove the old file if it exists
                if (!game.customCover.isEmpty()) {
                    val oldFile = File(game.customCover)
                    if (oldFile.exists() && oldFile.isFile) {
                        oldFile.delete()
                    }
                }

                val uniqueFileName = "picked_image_${game.id}.${getExtensionFromUri(context, uri)}"
                // Make a subdirectory for clean file hierarchy
                val coversDir = File(context.filesDir, "covers")
                if (!coversDir.exists()) {
                    coversDir.mkdirs()
                }
                val destinationFile = File(coversDir, uniqueFileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                // Update the game so that the custom cover appears
                gameLibraryRepository.updateGame(game.copy(customCover = destinationFile.absolutePath))

                // Return the file
                destinationFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun clearCoverArt(game: Game) {
        try {
            val oldFile = File(game.customCover)
            if (oldFile.exists() && oldFile.isFile) {
                oldFile.delete()
            }
            gameLibraryRepository.updateGame(game.copy(customCover = ""))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}

@Suppress("UNCHECKED_CAST")
class DetailViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val handle = extras.createSavedStateHandle()
        return DetailViewModel(container.gameLibraryRepository, handle) as T
    }
}