package com.example.collectalogger2.data.repository

import android.util.Log
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.GameDao
import com.example.collectalogger2.data.Genre
import com.example.collectalogger2.data.GenreDao
import com.example.collectalogger2.data.ImportIgnoredResult
import com.example.collectalogger2.data.ImportIgnoredResultDao
import com.example.collectalogger2.data.datasource.GameEvent
import com.example.collectalogger2.data.datasource.GenreDataSource
import com.example.collectalogger2.data.datasource.LocalDataSource
import com.example.collectalogger2.data.datasource.RemoteLibraryDataSource
import com.example.collectalogger2.data.datasource.searchIGDB
import com.example.collectalogger2.data.repository.RepositoryEvent.ShowErrorMessage
import com.example.collectalogger2.data.repository.RepositoryEvent.ShowInfoMessage
import com.example.collectalogger2.data.repository.RepositoryEvent.ShowLoadingFinished
import com.example.collectalogger2.data.repository.RepositoryEvent.ShowLoggedOut
import com.example.collectalogger2.data.repository.RepositoryEvent.ShowMissingGames
import com.example.collectalogger2.util.APIException
import com.example.collectalogger2.util.APIStatusException
import com.example.collectalogger2.util.AccountException
import com.example.collectalogger2.util.AccountExpiryException
import com.example.collectalogger2.util.PlayStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


// These events can be observed in the eventFlow
sealed class RepositoryEvent {
    data class ShowMissingGames(val gameMap: MutableMap<String, List<Game>>) : RepositoryEvent()
    data class ShowErrorMessage(val message: String, val errorMessage: String) : RepositoryEvent()
    data class ShowLoggedOut(val dataSource: RemoteLibraryDataSource) : RepositoryEvent()
    data class ShowInfoMessage(val message: String) : RepositoryEvent()
    object ShowLoadingFinished : RepositoryEvent()
}

class GameLibraryRepository(
    private val remoteLibraryDataSources: List<RemoteLibraryDataSource>,
    private val localDataSource: LocalDataSource?,
    private val genreDataSource: GenreDataSource,
    private val gameDao: GameDao,
    private val genreDao: GenreDao,
    private val importIgnoredResultDao: ImportIgnoredResultDao,
) {

    private var _genreFlow = MutableStateFlow<List<Genre>>(emptyList())
    var genreFlow: StateFlow<List<Genre>> = _genreFlow.asStateFlow()

    // The library games the user chose to ignore; they are excluded from future imports
    private var _ignoredResults = MutableStateFlow<List<ImportIgnoredResult>>(emptyList())
    var ignoredResults: StateFlow<List<ImportIgnoredResult>> = _ignoredResults.asStateFlow()

    // The load percentage as a float from 0-1, or -1 if not loading
    private var _loadPercentage = MutableStateFlow(-1f)
    var loadPercentage = _loadPercentage.asStateFlow()

    // For event emission
    private var _eventFlow = MutableSharedFlow<RepositoryEvent>()
    var eventFlow = _eventFlow.asSharedFlow()

    // The games that failed to import, grouped by the library they came from
    private var _missingGames = MutableStateFlow<Map<String, List<Game>>>(emptyMap())
    var missingGames: StateFlow<Map<String, List<Game>>> = _missingGames.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            genreDao.getAllGenresFlow().collect {
                _genreFlow.value = it
                if (_genreFlow.value.isEmpty()) {
                    genreDataSource.getGenres().forEach { genre ->
                        genreDao.insert(genre)
                        Log.i("GameLibraryRepository", "Genre inserted: ${genre.name}")
                    }
                }
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            importIgnoredResultDao.getAllIgnoredResultsFlow().collect {
                _ignoredResults.value = it
            }
        }
    }


    fun getGenreByIGDBId(id: Int): Genre? {
        // search through the list
        val filteredGenreList = genreFlow.value.filter { genre ->
            return@filter genre.igdbId == id
        }
        return if (filteredGenreList.isEmpty()) null
        else filteredGenreList[0]
    }

    fun getGenreById(id: Int): Genre? {
        // search through the list
        val filteredGenreList = genreFlow.value.filter { genre ->
            return@filter genre.id == id
        }
        return if (filteredGenreList.isEmpty()) null
        else filteredGenreList[0]
    }

    suspend fun insertGenre(genre: Genre) = genreDao.insert(genre)

    suspend fun deleteGenre(genre: Genre) = genreDao.delete(genre)

    suspend fun updateGenre(genre: Genre) = genreDao.update(genre)

    fun getAllGamesStream(): Flow<List<Game>> = gameDao.getAllGamesStream()

    fun getAllGames(): List<Game> = gameDao.getAllGames()

    fun getGameStream(id: Long): Flow<Game?> = gameDao.getGameStream(id)

    fun getGameById(id: Long): Game? = gameDao.getGame(id)

    fun getGameByIGDBId(igdbId: Long): Game? = gameDao.getGameByIGDBId(igdbId)

    fun getGameStreamByIGDBId(igdbId: Long): Flow<Game?> = gameDao.getGameStreamByIGDBId(igdbId)

    suspend fun insertGame(game: Game) = gameDao.insert(game)

    suspend fun deleteGame(game: Game) = gameDao.delete(game)

    suspend fun updateGame(game: Game) = gameDao.update(game)

    suspend fun insertIgnoredResult(ignoredResult: ImportIgnoredResult) =
        importIgnoredResultDao.insert(ignoredResult)

    suspend fun deleteIgnoredResult(ignoredResult: ImportIgnoredResult) =
        importIgnoredResultDao.delete(ignoredResult)

    /**
     * Searches IGDB for games by name.
     * Returns partial games, holding only the IGDB metadata, so they can be
     * matched up with a non-imported game with importNonImportedGame.
     */
    suspend fun searchIgdbGames(search: String, limit: Int = 5): List<Game> =
        searchIGDB(search, limit)

    /**
     * Imports a non-imported game by creating (or merging into, if an IGDB match
     * already exists) a full game. The IGDB metadata is taken from igdbGame, and
     * the store-specific data (store ID, playtime) from missingGame.
     */
    suspend fun importNonImportedGame(library: String, missingGame: Game, igdbGame: Game) {
        // The IGDB result holds no library data, so take the platform from the
        // missing game, defaulting to PC like the automatic import path
        val platform = missingGame.platform.ifEmpty { setOf("PC") }
        val existingGame = gameDao.getGameByIGDBId(igdbGame.igdbId)
        if (existingGame != null) {
            var newGame = existingGame.copy(
                playTime = maxOf(existingGame.playTime, missingGame.playTime),
                source = existingGame.source.plus(library),
                platform = existingGame.platform.plus(platform)
            )
            newGame = newGame.withStoreIdsFrom(missingGame)
            if (newGame.status.isEmpty()) {
                newGame = if (newGame.playTime > 0) {
                    newGame.copy(status = PlayStatus.Played)
                } else {
                    newGame.copy(status = PlayStatus.Unplayed)
                }
            }
            updateGame(newGame)
            Log.i(
                "Game merged!",
                "{title: ${newGame.title}, igdbId: ${newGame.igdbId}, id: ${newGame.id}}"
            )
        } else {
            var newGame = igdbGame.withStoreIdsFrom(missingGame).copy(
                playTime = missingGame.playTime,
                platform = platform,
                source = setOf(library)
            )
            newGame = if (newGame.playTime > 0) {
                newGame.copy(status = PlayStatus.Played)
            } else {
                newGame.copy(status = PlayStatus.Unplayed)
            }
            insertGame(newGame)
            Log.i(
                "Non-imported game imported!",
                "{title: ${newGame.title}, igdbId: ${newGame.igdbId}}"
            )
        }
    }

    /**
     * Copies the store-specific IDs of [other] into this game,
     * for the stores this game has none yet.
     */
    private fun Game.withStoreIdsFrom(other: Game): Game = copy(
        steamId = if (steamId != -1L) steamId else other.steamId,
        epicId = if (epicId.isNotEmpty()) epicId else other.epicId,
        gogId = if (gogId.isNotEmpty()) gogId else other.gogId,
        itchId = if (itchId.isNotEmpty()) itchId else other.itchId,
        amazonId = if (amazonId.isNotEmpty()) amazonId else other.amazonId,
    )

    fun getGamesBySearchTerm(search: String, limit: Int = 0): List<Game> =
        if (limit != 0)
            gameDao.getGamesSearchLimited(search, limit)
        else
            gameDao.getGamesSearch(search)

    suspend fun updateGameLibraries() {
        var newGames = 0
        val missingGamesMap = mutableMapOf<String, List<Game>>()
        _missingGames.value = emptyMap()
        Log.d("GameLibraryRepository", "Library data sources: $remoteLibraryDataSources")
        for (dataSource in remoteLibraryDataSources) {
            Log.d("GameLibraryRepository", "Starting import of ${dataSource.libraryName}")
            var retriesLeft = 3
            while (retriesLeft > 0) {
                // This block handles errors but is coarse-grained.
                // TODO add fine-grained failures in the getGames method(s).
                var expectedGamesCount = -1
                var actualGamesCount = 0
                try {
                    // reset the loading bar
                    _loadPercentage.value = 0f
                    val games = dataSource.getGames()
                    games
                        .collect { gameEvent ->
                            when (gameEvent) {
                                is GameEvent.GameLoaded -> {
                                    val game = gameEvent.game
                                    if (gameEvent.incrementGameCount) {
                                        // Increment the actualGamesCount for percentage bar
                                        actualGamesCount++
                                        // Update bar if appropriate
                                        if (expectedGamesCount != -1)
                                            _loadPercentage.value =
                                                (actualGamesCount.toFloat() / expectedGamesCount)
                                    }

                                    // See if it's here, if there is one
                                    val existingGame = gameDao.getGameByIGDBId(game.igdbId)
                                    if (existingGame != null) {
                                        // Update the game with the new information

                                        val highestPlaytime =
                                            maxOf(existingGame.playTime, game.playTime)
                                        var newGame = existingGame.copy(
                                            playTime = highestPlaytime,
                                            source = existingGame.source.plus(game.source),
                                            platform = existingGame.platform.plus(game.platform)
                                        )
                                        // Copy the game Id
                                        newGame = dataSource.copyWithID(newGame, game)

                                        if (newGame.status.isEmpty()) {
                                            newGame = if (game.playTime > 0) {
                                                newGame.copy(status = PlayStatus.Played)
                                            } else {
                                                newGame.copy(status = PlayStatus.Unplayed)
                                            }
                                        }
                                        updateGame(newGame)
                                        Log.i(
                                            "Game updated!",
                                            "{title: ${newGame.title}, igdbId: ${newGame.igdbId}, id: ${newGame.id}}"
                                        )
                                        Log.d(
                                            "Game updated extra!",
                                            "Additional information: {source: ${newGame.source}, steamId: ${newGame.steamId}, epicId: ${newGame.epicId}, platform: ${newGame.platform}}"
                                        )
                                    } else {
                                        newGames++
                                        // Set the basic play status based on playtime
                                        val newGame = if (game.playTime > 0) {
                                            game.copy(status = PlayStatus.Played)
                                        } else {
                                            game.copy(status = PlayStatus.Unplayed)
                                        }
                                        // Add the new game
                                        insertGame(newGame)
                                        Log.i(
                                            "Game added!",
                                            "{title: ${newGame.title}, igdbId: ${newGame.igdbId}, id: ${newGame.id}}"
                                        )
                                        Log.d(
                                            "Game added extra!",
                                            "Additional information: {source: ${newGame.source}, steamId: ${newGame.steamId}, epicId: ${newGame.epicId}, platform: ${newGame.platform}}"
                                        )
                                    }
                                }
                                is GameEvent.ExpectedGamesCount -> {
                                    expectedGamesCount = gameEvent.count
                                }
                                GameEvent.FinishGamesCount -> {
                                    _loadPercentage.value = 1f
                                }
                                is GameEvent.ListNonImportedGames -> {
                                    // Filter out results the user chose to ignore,
                                    // so they won't be reported (or imported) in the future
                                    val seenTitles = mutableSetOf<String>()
                                    val reportedGames = gameEvent.games.filterNot { game ->
                                        _ignoredResults.value.any { ignored ->
                                            ignored.library == dataSource.libraryName &&
                                                    ignored.name.equals(game.title, ignoreCase = true)
                                        }
                                    }.filter { game ->
                                        // Combine duplicates (e.g. multiple entitlements of the
                                        // same game on Amazon), so they are only listed once
                                        val title = game.title.trim()
                                        title.isEmpty() || seenTitles.add(title.lowercase())
                                    }
                                    if (reportedGames.isNotEmpty()) {
                                        missingGamesMap[dataSource.libraryName] = reportedGames
                                        _missingGames.value = missingGamesMap.toMap()
                                    }
                                }

                                GameEvent.IncrementGamesCount -> {
                                    // Increment the actualGamesCount for percentage bar
                                    actualGamesCount++
                                    if (expectedGamesCount != -1)
                                        _loadPercentage.value =
                                            (actualGamesCount.toFloat() / expectedGamesCount)

                                }
                            }
                        }
                }
                catch (e: Exception) {
                    if (e is APIStatusException) {
                        Log.e("APIStatusException happened with HTTP code ${e.statusCode}", e.message ?: "")
                        _eventFlow.emit(
                            ShowErrorMessage(
                                "APIStatusException: HTTP Error ${e.statusCode}",
                                e.message ?: ""
                            )
                        )
                    } else if (e is APIException) {
                        Log.e("APIException happened", e.message ?: "")
                        _eventFlow.emit(
                            ShowErrorMessage(
                                "APIException", e.message ?: ""
                            )
                        )
                    } else if (e is AccountExpiryException) {
                        Log.e("AccountExpiryException happened", e.message ?: "")
                        // Instead of emitting an exception, emit a logged out event
                        _eventFlow.emit(ShowLoggedOut(dataSource))
                        break // go to the next DataSource
                    } else if (e is AccountException) {
                        // If the user is not logged in
                        Log.e("AccountException happened", e.message ?: "")
                        // We don't want to complain about a source the user has never logged into,
                        // so skip
                        break
                    }
                    else {
                        Log.e("Exception happened", e.message ?: "")
                        throw e
                    }
                    retriesLeft--
                    delay(5000.milliseconds) // Retry with delay
                    continue
                }
                break
            }
        }
        Log.d("GameLibraryRepository", "Finished importing all games!")
        // either emit the missingGamesMap or the number of games
        if (missingGamesMap.isNotEmpty()) {
            _eventFlow.emit(ShowMissingGames(missingGamesMap))
        } else if (newGames != 0) { // Don't tell if nothing happens
            _eventFlow.emit(ShowInfoMessage("Imported $newGames new games"))
        }
        _eventFlow.emit(ShowLoadingFinished)
        _loadPercentage.value = -1f
    }

}