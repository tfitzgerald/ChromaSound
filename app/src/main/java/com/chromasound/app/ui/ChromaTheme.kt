package com.chromasound.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color

/**
 * All theme-sensitive colours and blend mode in one place.
 * Access via LocalChromaTheme.current anywhere in the Compose tree.
 */
data class ChromaColors(
    val isDark:       Boolean,
    val bgColor:      Color,   // canvas / screen background
    val bgCard:       Color,   // card surface
    val uiAccent:     Color,   // purple accent
    val uiText:       Color,   // primary text
    val uiSubtle:     Color,   // secondary / muted text
    val shapeBlend:   BlendMode // Screen for dark, Multiply for light
)

val DarkChromaColors = ChromaColors(
    isDark     = true,
    bgColor    = Color(0xFF050508),
    bgCard     = Color(0xFF0F0F1A),
    uiAccent   = Color(0xFF7C6FFF),
    uiText     = Color(0xFFE0DFF8),
    uiSubtle   = Color(0xFF5A5870),
    shapeBlend = BlendMode.Screen
)

val LightChromaColors = ChromaColors(
    isDark     = false,
    bgColor    = Color(0xFFF5F5FA),
    bgCard     = Color(0xFFEAEAF5),
    uiAccent   = Color(0xFF5B4ECC),
    uiText     = Color(0xFF1A1830),
    uiSubtle   = Color(0xFF6B6890),
    shapeBlend = BlendMode.Multiply
)

val LocalChromaTheme = compositionLocalOf { DarkChromaColors }
