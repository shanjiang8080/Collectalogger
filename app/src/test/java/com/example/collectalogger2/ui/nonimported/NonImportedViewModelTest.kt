package com.example.collectalogger2.ui.nonimported

import app.cash.turbine.test
import com.example.collectalogger2.MainDispatcherRule
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.ImportIgnoredResult
import com.example.collectalogger2.data.repository.GameLibraryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NonImportedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockGameRepository = mockk<GameLibraryRepository>(relaxed = true)

    // We use actual flows to control data emission
    private val missingGamesStream =
        MutableStateFlow<Map<String, List<Game>>>(emptyMap())
    private val ignoredResultsStream =
        MutableStateFlow<List<ImportIgnoredResult>>(emptyList())

    private lateinit var viewModel: NonImportedViewModel

    @Before
    fun setup() {
        every { mockGameRepository.missingGames } returns missingGamesStream
        every { mockGameRepository.ignoredResults } returns ignoredResultsStream
        viewModel = NonImportedViewModel(mockGameRepository)
    }

    @Test
    fun `missingGames flow populates uiState grouped by library`() = runTest {
        val games = mapOf(
            "Steam" to listOf(Game(title = "Weird Game"), Game(title = "Another One")),
            "Epic Games" to listOf(Game(title = "Some Game"))
        )

        viewModel.uiState.test {
            assertEquals(emptyMap<String, List<Game>>(), awaitItem().gamesByLibrary)

            missingGamesStream.value = games

            assertEquals(games, awaitItem().gamesByLibrary)
        }
    }

    @Test
    fun `selection toggle adds and removes keys correctly`() = runTest {
        viewModel.uiState.test {
            assertEquals(emptySet<String>(), awaitItem().selectedKeys) // Initial

            viewModel.toggleSelection("Steam:Weird Game")
            assertTrue(awaitItem().selectedKeys.contains("Steam:Weird Game"))

            viewModel.toggleSelection("Steam:Weird Game")
            assertTrue(awaitItem().selectedKeys.isEmpty())
        }
    }

    @Test
    fun `ignoreSelected inserts selected results and clears selection`() = runTest {
        val game = Game(title = "Weird Game")
        missingGamesStream.value = mapOf("Steam" to listOf(game))
        viewModel.toggleSelection(selectionKey("Steam", game))

        viewModel.ignoreSelected()

        coVerify {
            mockGameRepository.insertIgnoredResult(
                ImportIgnoredResult(name = "Weird Game", library = "Steam")
            )
        }
        assertTrue(viewModel.uiState.value.selectedKeys.isEmpty())
    }

    @Test
    fun `ignored results are filtered out of the displayed list`() = runTest {
        missingGamesStream.value = mapOf(
            "Steam" to listOf(Game(title = "Weird Game"), Game(title = "Another One"))
        )

        ignoredResultsStream.value = listOf(
            ImportIgnoredResult(name = "weird game", library = "Steam")
        )

        val displayed = viewModel.uiState.value.gamesByLibrary
        assertEquals(listOf("Another One"), displayed["Steam"]?.map { it.title })
    }

    @Test
    fun `libraries with no remaining games are removed from the displayed list`() = runTest {
        missingGamesStream.value = mapOf(
            "Steam" to listOf(Game(title = "Weird Game")),
            "GOG" to listOf(Game(title = "Some Game"))
        )

        ignoredResultsStream.value = listOf(
            ImportIgnoredResult(name = "Weird Game", library = "Steam")
        )

        assertEquals(setOf("GOG"), viewModel.uiState.value.gamesByLibrary.keys)
    }

    @Test
    fun `openSearchDialog fetches IGDB results for each selected game`() = runTest {
        val game = Game(title = "Weird Game")
        missingGamesStream.value = mapOf("Steam" to listOf(game))
        val igdbResults = listOf(Game(igdbId = 1, title = "The Actual Weird Game"))
        coEvery { mockGameRepository.searchIgdbGames("Weird Game", any()) } returns igdbResults

        viewModel.toggleSelection(selectionKey("Steam", game))
        viewModel.openSearchDialog()

        val state = viewModel.uiState.value
        assertTrue(state.showSearchDialog)
        assertEquals(igdbResults, state.searchResults[selectionKey("Steam", game)])
    }

    @Test
    fun `selectionKey disambiguates results with the same title`() {
        val first = Game(title = "Metal Slug 3", amazonId = "amzn1.1")
        val second = Game(title = "Metal Slug 3", amazonId = "amzn1.2")

        assertNotEquals(selectionKey("Amazon", first), selectionKey("Amazon", second))
    }

    @Test
    fun `confirmSearchDialog imports chosen results and removes them from the screen`() = runTest {
        val game = Game(title = "Weird Game", playTime = 10, steamId = 123)
        missingGamesStream.value = mapOf("Steam" to listOf(game))
        val chosen = Game(igdbId = 7, title = "The Actual Weird Game", publishers = setOf("Pub"))

        viewModel.toggleSelection(selectionKey("Steam", game))
        viewModel.chooseIgdbResult(selectionKey("Steam", game), chosen)
        viewModel.confirmSearchDialog()

        // confirmSearchDialog runs on Dispatchers.IO, so wait for it to complete
        coVerify(timeout = 5000) {
            mockGameRepository.importNonImportedGame("Steam", game, chosen)
        }
        waitFor(timeout = 5000) { viewModel.uiState.value.gamesByLibrary.isEmpty() }
        val state = viewModel.uiState.value
        assertFalse(state.showSearchDialog)
        assertTrue(state.selectedKeys.isEmpty())
        assertTrue(state.gamesByLibrary.isEmpty())
    }

    // Busy-waits for a condition, for flows that update from Dispatchers.IO
    private fun waitFor(timeout: Long, condition: () -> Boolean) {
        val startTime = System.currentTimeMillis()
        while (!condition()) {
            if (System.currentTimeMillis() - startTime > timeout) {
                throw AssertionError("Condition was not met within $timeout ms")
            }
            Thread.sleep(10)
        }
    }
}
