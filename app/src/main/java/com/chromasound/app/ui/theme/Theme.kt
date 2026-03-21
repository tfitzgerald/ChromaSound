package com.chromasound.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val chromaColorScheme = darkColorScheme(
    primary   = Color(0xFF7C6FFF),
    secondary = Color(0xFF42E5F5),
    background = Color(0xFF050508),
    surface    = Color(0xFF0A0A14),
    onPrimary  = Color.White,
    onBackground = Color(0xFFE0DFF8),
    onSurface  = Color(0xFFE0DFF8)
)

@Composable
fun ChromaSoundTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = chromaColorScheme,
        content = content
    )
}
