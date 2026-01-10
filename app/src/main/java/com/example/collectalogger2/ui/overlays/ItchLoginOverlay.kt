package com.example.collectalogger2.ui.overlays

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri

private const val oAuthUrl =
    "https://itch.io/user/oauth?client_id=9c390053f3ea6f87367c0dfbbbad00f7&scope=profile:owned&redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=token"

@Composable
fun ItchOverlay(
    onDismiss: () -> Unit,
    saveItchSecret: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Login with OAuth to Itch.io. " +
                        "Once done, paste the resulting key into the text-box.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            ) // TODO XML-ize this
            var itchSecret by remember { mutableStateOf("") }
            val ctx = LocalContext.current
            Button(
                onClick = {
                    val urlIntent = Intent(
                        Intent.ACTION_VIEW,
                        oAuthUrl.toUri()
                    )
                    ctx.startActivity(urlIntent)
                },
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(
                    text = "Log in via OAuth",
                    modifier = Modifier.padding(10.dp),
                )
            }
            TextField(
                value = itchSecret,
                onValueChange = { itchSecret = it },
                label = { Text("Itch OAuth Key") },
                placeholder = { Text("ABCDEF...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(),
                    onClick = {
                        // set the ID appropriately!
                        saveItchSecret(itchSecret)
                        onDismiss()
                    }
                ) {
                    Text("Save")
                }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    onClick = {
                        onDismiss()
                    }) {
                    Text("Cancel")
                }

            }

        }
    }

}

@Preview
@Composable
fun ItchOverlayPreview() {
    ItchOverlay(onDismiss = {}, {})
}

