package com.chromasound.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * All theme-sensitive colours for UI chrome (settings, HUD, cards, text).
 * Note: the visualiser CANVAS is always dark regardless of theme —
 * BlendMode.Screen requires a dark background to render shapes correctly.
 * Light theme applies only to the surrounding UI elements.
 */
data class ChromaColors(
    val isDark:     Boolean,
    val bgColor:    Color,  // canvas / screen background (always dark for visualiser)
    val bgCard:     Color,  // card / surface colour
    val uiAccent:   Color,  // purple accent
    val uiText:     Color,  // primary text
    val uiSubtle:   Color   // secondary / muted text
)

val DarkChromaColors = ChromaColors(
    isDark   = true,
    bgColor  = Color(0xFF050508),
    bgCard   = Color(0xFF0F0F1A),
    uiAccent = Color(0xFF7C6FFF),
    uiText   = Color(0xFFE0DFF8),
    uiSubtle = Color(0xFF5A5870)
)

val LightChromaColors = ChromaColors(
    isDark   = false,
    bgColor  = Color(0xFFF5F5FA),
    bgCard   = Color(0xFFEAEAF5),
    uiAccent = Color(0xFF5B4ECC),
    uiText   = Color(0xFF1A1830),
    uiSubtle = Color(0xFF6B6890)
)

val LocalChromaTheme = compositionLocalOf { DarkChromaColors }
