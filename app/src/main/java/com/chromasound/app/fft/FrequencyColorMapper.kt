package com.chromasound.app.fft

import androidx.compose.ui.graphics.Color
import com.chromasound.app.model.ColorScheme

/**
 * Maps a frequency in Hz to a fully-opaque colour.
 *
 * RAINBOW        Bass (30 Hz) = violet  →  Treble (11 kHz) = red
 * INVERSE_RAINBOW Bass (30 Hz) = red    →  Treble (11 kHz) = violet
 *
 * Both schemes traverse the full visible-light hue wheel so every band
 * always gets a distinct, saturated colour.
 */
object FrequencyColorMapper {

    private const val MIN_HZ =   30f
    private const val MAX_HZ = 11_000f

    /**
     * Return the color for [bandIndex] / [hz], checking [overrides] first.
     * If an override exists for the band it is returned directly (alpha = 1f).
     * Otherwise the automatic hue mapping via [scheme] is used.
     */
    fun colorForBand(
        bandIndex: Int,
        hz:        Float,
        scheme:    ColorScheme       = ColorScheme.RAINBOW,
        overrides: Map<Int, androidx.compose.ui.graphics.Color> = emptyMap()
    ): androidx.compose.ui.graphics.Color {
        overrides[bandIndex]?.let { return it.copy(alpha = 1f) }
        return frequencyToColor(hz, scheme)
    }

    fun frequencyToColor(hz: Float, scheme: ColorScheme = ColorScheme.RAINBOW): androidx.compose.ui.graphics.Color {
    fun frequencyToColor(hz: Float, scheme: ColorScheme = ColorScheme.RAINBOW): androidx.compose.ui.graphics.Color {
        val logMin = Math.log10(MIN_HZ.toDouble())
        val logMax = Math.log10(MAX_HZ.toDouble())
        val logHz  = Math.log10(hz.toDouble().coerceIn(MIN_HZ.toDouble(), MAX_HZ.toDouble()))
        val t = ((logHz - logMin) / (logMax - logMin)).toFloat().coerceIn(0f, 1f)
        val tMapped = if (scheme == ColorScheme.INVERSE_RAINBOW) 1f - t else t
        val hue = (270f - tMapped * 270f).let { h -> ((h % 360f) + 360f) % 360f }
        return hsvToColor(hue, saturation = 1f, value = 1f)
    }

    fun hsvToColor(hue: Float, saturation: Float, value: Float): androidx.compose.ui.graphics.Color {
        val h = hue / 60f
        val i = h.toInt()
        val f = h - i
        val p = value * (1f - saturation)
        val q = value * (1f - saturation * f)
        val t = value * (1f - saturation * (1f - f))
        val (r, g, b) = when (i % 6) {
            0    -> Triple(value, t, p)
            1    -> Triple(q, value, p)
            2    -> Triple(p, value, t)
            3    -> Triple(p, q, value)
            4    -> Triple(t, p, value)
            else -> Triple(value, p, q)
        }
        return Color(r, g, b, 1f)
    }
}
