package com.example.collectalogger2.ui.settings

import app.cash.turbine.test
import com.example.collectalogger2.MainDispatcherRule
import com.example.collectalogger2.data.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: SettingsViewModel
    private val mockRepository = mockk<SettingsRepository>(relaxed = true)

    @Before
    fun setup() {
        // Mock repository flows to return some initial values
        every { mockRepository.steamId } returns flowOf("initial_steam")
        every { mockRepository.epicIdInfo } returns flowOf("initial_epic")
        every { mockRepository.gogUsername } returns flowOf("initial_gog")
        every { mockRepository.itchSecret } returns flowOf("initial_itch")
        every { mockRepository.amazonIdInfo } returns flowOf("initial_amazon")

        viewModel = SettingsViewModel(mockRepository)
    }

    @Test
    fun `init loads settings from repository`() = runTest {
        viewModel.steamId.test {
            assertEquals("initial_steam", awaitItem())
        }
        viewModel.epicInfo.test {
            assertEquals("initial_epic", awaitItem())
        }
        viewModel.gogUsername.test {
            assertEquals("initial_gog", awaitItem())
        }
        viewModel.itchSecret.test {
            assertEquals("initial_itch", awaitItem())
        }
        viewModel.amazonInfo.test {
            assertEquals("initial_amazon", awaitItem())
        }
    }

    @Test
    fun `setStoreFront updates currentStoreFront state`() = runTest {
        viewModel.currentStoreFront.test {
            assertEquals("", awaitItem())
            viewModel.setStoreFront("Steam")
            assertEquals("Steam", awaitItem())
        }
    }

    @Test
    fun `saveSteamId calls getSteamLogin`() = runTest {
        mockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
        coEvery { getSteamLogin(any(), any()) } returns Unit

        val testUrl = "https://steamcommunity.com/id/test"
        viewModel.saveSteamId(testUrl)

        coVerify { getSteamLogin(testUrl, mockRepository) }
        unmockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
    }

    @Test
    fun `saveEpicInfo calls getEpicLogin`() = runTest {
        mockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
        coEvery { getEpicLogin(any(), any()) } returns Unit

        val testCode = "epic_code"
        viewModel.saveEpicInfo(testCode)

        coVerify { getEpicLogin(testCode, mockRepository) }
        unmockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
    }

    @Test
    fun `saveGogUsername calls getGogLogin`() = runTest {
        mockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
        coEvery { getGogLogin(any(), any()) } returns Unit

        val testUser = "gog_user"
        viewModel.saveGogUsername(testUser)

        coVerify { getGogLogin(testUser, mockRepository) }
        unmockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
    }

    @Test
    fun `saveItchSecret calls getItchLogin`() = runTest {
        mockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
        coEvery { getItchLogin(any(), any()) } returns Unit

        val testSecret = "itch_secret"
        viewModel.saveItchSecret(testSecret)

        coVerify { getItchLogin(testSecret, mockRepository) }
        unmockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
    }

    @Test
    fun `saveAmazonInfo calls getAmazonLogin`() = runTest {
        mockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
        coEvery { getAmazonLogin(any(), any(), any()) } returns Unit

        val testCode = "amazon_code"
        val testVerifier = "amazon_verifier"
        viewModel.saveAmazonInfo(testCode, testVerifier)

        coVerify { getAmazonLogin(testCode, testVerifier, mockRepository) }
        unmockkStatic("com.example.collectalogger2.ui.settings.LibraryLogInsKt")
    }
}
