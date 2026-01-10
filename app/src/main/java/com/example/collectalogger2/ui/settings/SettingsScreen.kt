package com.example.collectalogger2.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.collectalogger2.R
import com.example.collectalogger2.ui.overlays.EpicOverlay
import com.example.collectalogger2.ui.overlays.GogOverlay
import com.example.collectalogger2.ui.overlays.ItchOverlay
import com.example.collectalogger2.ui.overlays.SteamOverlay

// UPDATE WHEN ADDING LIBRARIES
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val steamId by viewModel.steamId.collectAsStateWithLifecycle()
    val epicInfo by viewModel.epicInfo.collectAsStateWithLifecycle()
    val gogUsername by viewModel.gogUsername.collectAsStateWithLifecycle()
    val itchSecret by viewModel.itchSecret.collectAsStateWithLifecycle()
    val activeStoreFront by viewModel.currentStoreFront.collectAsStateWithLifecycle()

    Column {
        Text(
            text = "Log in to services:", // TODO make XML-ized
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row {
            // make buttons for each library, like Steam, Epic, etc.
            GameLibraryButton(
                R.drawable.logo_steam,
                "Steam button",
                steamId.isEmpty()
            ) { viewModel.setStoreFront("Steam") }
            GameLibraryButton(
                R.drawable.logo_egs,
                "Epic Games button",
                epicInfo.isEmpty()
            ) { viewModel.setStoreFront("Epic") }
            GameLibraryButton(
                R.drawable.logo_gog,
                "GOG button",
                gogUsername.isEmpty()
            ) { viewModel.setStoreFront("GOG") }
            GameLibraryButton(
                R.drawable.logo_itch,
                "Itch button",
                itchSecret.isEmpty()
            ) { viewModel.setStoreFront("Itch") }
        }
    }
    val onDismiss = { viewModel.setStoreFront("") }
    when (activeStoreFront) {
        "Steam" -> SteamOverlay(
            onDismiss = onDismiss,
            saveSteamID = { url -> viewModel.saveSteamId(url) })

        "Epic" -> EpicOverlay(
            onDismiss = onDismiss,
            saveEpicID = { info -> viewModel.saveEpicInfo(info) })

        "GOG" -> GogOverlay(
            onDismiss = onDismiss,
            saveGogUsername = { username -> viewModel.saveGogUsername(username) }
        )
        "Itch" -> ItchOverlay(
            onDismiss = onDismiss,
            saveItchSecret = { secret -> viewModel.saveItchSecret(secret) }
        )
        else -> {}

    }
}


@Composable
fun GameLibraryButton(
    icon: Int,
    description: String,
    isLoggedIn: Boolean,
    onClick: () -> Unit
) {
    // create a button with onclick and stuff
    if (isLoggedIn) {
        Button(
            onClick = onClick,
            enabled = true,
            modifier = Modifier.size(width = 80.dp, height = 80.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = description,
            )
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            enabled = true,
            modifier = Modifier.size(width = 80.dp, height = 80.dp),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = description,
            )
        }

    }
}