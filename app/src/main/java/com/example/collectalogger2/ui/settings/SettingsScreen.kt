package com.example.collectalogger2.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
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

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val steamId by viewModel.steamId.collectAsStateWithLifecycle()
    val activeStoreFront by viewModel.currentStoreFront.collectAsStateWithLifecycle()

    Column() {
        Text(
            text = "Log in to services:", // TODO make XML-ized
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row() {
            // make buttons for each library, like Steam, Epic, etc.
            GameLibraryButton(
                R.drawable.logo_steam,
                "Steam button",
                steamId.isEmpty()
            ) { viewModel.setStoreFront("Steam") }
        }
    }

    when (activeStoreFront) {
        "Steam" -> SteamOverlay(onDismiss = { viewModel.setStoreFront("") }, saveSteamID = { url -> viewModel.saveSteamId(url) })
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
    Button(
        onClick = onClick,
        enabled = true,
        modifier = Modifier.size(width = 80.dp, height = 80.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description
            )
    }
}