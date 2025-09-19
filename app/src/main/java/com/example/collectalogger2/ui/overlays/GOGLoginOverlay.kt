package com.example.collectalogger2.ui.overlays

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun GogOverlay(
    onDismiss: () -> Unit,
    saveGogUsername: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Enter your GOG username.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            ) // TODO XML-ize this
            var gogUsername by remember { mutableStateOf("") }
            TextField(
                value = gogUsername,
                onValueChange = { gogUsername = it },
                label = { Text("GOG Username") },
                placeholder = { Text("Username") },
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
                        saveGogUsername(gogUsername)
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
fun GogOverlayPreview() {
    GogOverlay(onDismiss = {}, {})
}
