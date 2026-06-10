package com.example.collectalogger2.ui.gallery

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.collectalogger2.MainDispatcherRule
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.Genre
import com.example.collectalogger2.data.datasource.EpicDataSource
import com.example.collectalogger2.data.repository.GameLibraryRepository
import com.example.collectalogger2.data.repository.RepositoryEvent
import com.example.collectalogger2.data.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GalleryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockGameRepository = mockk<GameLibraryRepository>(relaxed = true)
    private val mockSettingsRepository = mockk<SettingsRepository>(relaxed = true)

    // We use actual flows to control data emission
    private val gameStream = MutableStateFlow<List<Game>>(emptyList())
    private val genreStream = MutableStateFlow<List<Genre>>(emptyList())
    private val eventStream = MutableSharedFlow<RepositoryEvent>()

    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0

        // Link repository methods to our test flows
        every { mockGameRepository.getAllGamesStream() } returns gameStream
        every { mockGameRepository.genreFlow } returns genreStream
        every { mockGameRepository.eventFlow } returns eventStream

        // Initialize with empty SavedStateHandle by default
        viewModel = GalleryViewModel(
            gameLibraryRepository = mockGameRepository,
            settingsRepository = mockSettingsRepository,
            savedStateHandle = SavedStateHandle()
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `init correctly prepares filter from savedStateHandle`() = runTest {
        // Create a new VM specifically for this test to check init logic
        val handle = SavedStateHandle(
            mapOf(
                "library" to "Steam",
                "isFavorite" to true
            )
        )

        val vmWithArgs = GalleryViewModel(mockGameRepository, mockSettingsRepository, handle)

        vmWithArgs.uiState.test {
            val state = awaitItem()
            assertEquals(listOf("Steam"), state.filter?.library)
            assertEquals(true, state.filter?.isFavorite)
        }
    }

    @Test
    fun `selection toggle adds and removes IDs correctly`() = runTest {
        viewModel.uiState.test {
            assertEquals(emptySet<Long>(), awaitItem().selectedIds) // Initial

            viewModel.toggleSelection(101L)
            assertTrue(awaitItem().selectedIds.contains(101L))

            viewModel.toggleSelection(101L)
            assertTrue(awaitItem().selectedIds.isEmpty())
        }
    }

    @Test
    fun `getSearchedGames filters cached games by title`() = runTest {
        // 1. Fill the cache
        val game1 = Game(id = 1, title = "Hades")
        val game2 = Game(id = 2, title = "Halo")
        val game3 = Game(id = 3, title = "Doom")
        gameStream.value = listOf(game1, game2, game3)

        viewModel.uiState.test {
            awaitItem() // Skip initial empty state

            // 2. Search
            viewModel.getSearchedGames("Ha")

            val state = awaitItem()
            assertEquals(2, state.games.size)
            assertTrue(state.games.any { it.title == "Hades" })
            assertTrue(state.games.any { it.title == "Halo" })
        }
    }

    @Test
    fun `repository LoggedOut event triggers ShowDialog UI event`() = runTest {
        viewModel.uiEvents.test {
            // Simulate repository sending a LoggedOut event
            val mockDataSource = mockk<EpicDataSource>()
            every { mockDataSource.libraryName } returns "Epic"

            eventStream.emit(RepositoryEvent.ShowLoggedOut(mockDataSource))

            val event = awaitItem()
            assertTrue(event is UiEvent.ShowDialog)
            val dialogEvent = event as UiEvent.ShowDialog
            assertTrue(dialogEvent.actionType is DialogActionType.LoggedOut)
            assertEquals("Epic", (dialogEvent.actionType as DialogActionType.LoggedOut).library)
        }
    }

    @Test
    fun `deleteGames purges database and clears UI state`() = runTest {
        val game = Game(id = 1, title = "To be deleted")
        coEvery { mockGameRepository.getAllGames() } returns listOf(game)

        viewModel.deleteGames()

        // Verify repository calls
        coVerify { mockGameRepository.deleteGame(game) }

        // Verify UI reflects empty state
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.games.isEmpty())
        }
    }
}