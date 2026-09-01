package com.example.collectalogger2.ui.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.collectalogger2.MainDispatcherRule
import com.example.collectalogger2.data.Game
import com.example.collectalogger2.data.Genre
import com.example.collectalogger2.data.repository.GameLibraryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: DetailViewModel

    // Use the actual repository interface/class name you refactored to
    private val mockRepository = mockk<GameLibraryRepository>(relaxed = true)

    @Before
    fun setup() {
        // 1. Use MutableStateFlow to satisfy the StateFlow requirement
        // and explicitly define the List<Genre> type.
        val mockGenreFlow = MutableStateFlow(emptyList<Genre>())

        // 2. Mock the repository to return these specific flows
        every { mockRepository.getGameStream(any()) } returns flowOf(null)
        every { mockRepository.genreFlow } returns mockGenreFlow

        // Initialize the ViewModel
        viewModel = DetailViewModel(
            gameLibraryRepository = mockRepository,
            savedStateHandle = SavedStateHandle(mapOf("id" to 1L))
        )
    }

    @Test
    fun `setDialog updates currentDialog state`() = runTest {
        viewModel.currentDialog.test {
            // Check initial state
            assertEquals("", awaitItem())

            // Trigger change
            viewModel.setDialog("TimePlayed")

            // Check updated state
            assertEquals("TimePlayed", awaitItem())
        }
    }

    @Test
    fun `init loads game and fetches corresponding genres`() = runTest {
        // 1. Prepare data
        val testGame =
            Game(id = 1L, title = "Test Game", genre = setOf(10, 20))
        val genre1 = Genre(id = 1, igdbId = 10, name = "RPG")
        val genre2 = Genre(id = 2, igdbId = 20, name = "Action")

        // 2. Mock repository behavior
        every { mockRepository.getGameStream(1L) } returns flowOf(testGame)
        every { mockRepository.getGenreByIGDBId(10) } returns genre1
        every { mockRepository.getGenreByIGDBId(20) } returns genre2

        // 3. Re-initialize ViewModel to trigger init block with this data
        viewModel = DetailViewModel(mockRepository, SavedStateHandle(mapOf("id" to 1L)))

        // 4. Verify StateFlows
        viewModel.game.test {
            assertEquals("Test Game", awaitItem()?.title)
        }
        viewModel.gameGenres.test {
            val genres = awaitItem()
            assertEquals(2, genres.size)
            assertEquals("RPG", genres[0].name)
        }
    }

    @Test
    fun `toggleFavoriteGame calls repository with flipped favorite status`() = runTest {
        // 1. Setup a game that is currently NOT favorited
        val testGame = Game(id = 1L, title = "Test Game", isFavorite = false)
        every { mockRepository.getGameStream(1L) } returns flowOf(testGame)

        // Initialize
        viewModel = DetailViewModel(mockRepository, SavedStateHandle(mapOf("id" to 1L)))

        // 2. Execute Action
        viewModel.toggleFavoriteGame()

        // 3. Verify repository was called with isFavorite = true
        io.mockk.coVerify {
            mockRepository.updateGame(match { it.isFavorite })
        }
    }

    @Test
    fun `onSubmitChanges cleans data and updates repository`() = runTest {
        val testGame = Game(id = 1L, title = "Old Title")
        every { mockRepository.getGameStream(1L) } returns flowOf(testGame)
        viewModel = DetailViewModel(mockRepository, SavedStateHandle(mapOf("id" to 1L)))

        val newGenres = listOf(Genre(id = 1, igdbId = 99, name = "Sci-Fi"))

        viewModel.onSubmitChanges(
            title = "New Title",
            sortingName = "New Title",
            description = "Desc",
            developers = setOf("Dev A", ""), // Note the empty string
            publishers = setOf("Pub A"),
            genres = newGenres
        )

        io.mockk.coVerify {
            mockRepository.updateGame(match {
                it.title == "New Title" &&
                        it.developers.size == 1 && // Empty string should be gone
                        it.genre.contains(99)      // Mapped from igdbId
            })
        }
    }

    @Test
    fun `clearCoverArt updates game with empty cover path`() = runTest {
        // Setup game with an existing cover
        val testGame = Game(id = 1L, title = "Test", customCover = "path/to/image.jpg")
        every { mockRepository.getGameStream(1L) } returns flowOf(testGame)
        viewModel = DetailViewModel(mockRepository, SavedStateHandle(mapOf("id" to 1L)))

        viewModel.clearCoverArt(testGame)

        // Verify updateGame was called and the cover is now empty
        io.mockk.coVerify {
            mockRepository.updateGame(match { it.customCover.isEmpty() })
        }
    }

    @Test
    fun `setDialog to empty string effectively closes dialog`() = runTest {
        viewModel.currentDialog.test {
            assertEquals("", awaitItem()) // Initial

            viewModel.setDialog("TimePlayed")
            assertEquals("TimePlayed", awaitItem())

            viewModel.setDialog("")
            assertEquals("", awaitItem())
        }
    }

}