package com.example.collectalogger2.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.collectalogger2.R

/**
 * A button with text and an icon to the left of it.
 */
@Composable
fun AnnotatedButton(
    onClick: () -> Unit,
    iconPainter: Painter,
    label: String,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = colors
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = label
            )
            Text(label)
        }
    }
}

@Preview
@Composable
private fun AnnotatedButtonColorsPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnnotatedButton(
            onClick = {},
            iconPainter = painterResource(id = R.drawable.mic_save),
            label = "Test",
            colors = ButtonDefaults.buttonColors()
        )
        AnnotatedButton(
            onClick = {},
            iconPainter = painterResource(id = R.drawable.mic_save),
            label = "Test",
            colors = ButtonDefaults.textButtonColors()
        )
        AnnotatedButton(
            onClick = {},
            iconPainter = painterResource(id = R.drawable.mic_save),
            label = "Test",
            colors = ButtonDefaults.elevatedButtonColors()
        )
        AnnotatedButton(
            onClick = {},
            iconPainter = painterResource(id = R.drawable.mic_save),
            label = "Test",
            colors = ButtonDefaults.filledTonalButtonColors()
        )
        AnnotatedButton(
            onClick = {},
            iconPainter = painterResource(id = R.drawable.mic_save),
            label = "Test",
            colors = ButtonDefaults.outlinedButtonColors()
        )

    }
}