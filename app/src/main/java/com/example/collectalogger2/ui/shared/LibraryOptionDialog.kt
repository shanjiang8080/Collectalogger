package com.example.collectalogger2.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * A reusable question dialog shown before opening a library's login overlay,
 * asking about library-specific import options (e.g. whether Nintendo should
 * also import physical games with playtime).
 */
data class LibraryDialogOption(
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun LibraryOptionDialog(
    title: String,
    text: String,
    options: List<LibraryDialogOption>,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Column {
                options.forEach { option ->
                    TextButton(onClick = option.onClick) {
                        Text(option.label)
                    }
                }
            }
        }
    )
}
