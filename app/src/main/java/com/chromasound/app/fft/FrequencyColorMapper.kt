package com.chromasound.app.fft

import androidx.compose.ui.graphics.Color
import com.chromasound.app.model.ColorScheme

object FrequencyColorMapper {

    private const val MIN_HZ =   30f
    private const val MAX_HZ = 11_000f

    /** Return the colour for [bandIndex], checking [overrides] before the auto scheme. */
    fun colorForBand(
        bandIndex: Int,
        hz:        Float,
        scheme:    ColorScheme      = ColorScheme.RAINBOW,
        overrides: Map<Int, Color>  = emptyMap()
    ): Color {
        overrides[bandIndex]?.let { return it.copy(alpha = 1f) }
        return frequencyToColor(hz, scheme)
    }

    /** Convert a frequency in Hz to a fully-opaque Color using the given color scheme. */
    fun frequencyToColor(hz: Float, scheme: ColorScheme = ColorScheme.RAINBOW): Color {
        val logMin  = Math.log10(MIN_HZ.toDouble())
        val logMax  = Math.log10(MAX_HZ.toDouble())
        val logHz   = Math.log10(hz.toDouble().coerceIn(MIN_HZ.toDouble(), MAX_HZ.toDouble()))
        val t       = ((logHz - logMin) / (logMax - logMin)).toFloat().coerceIn(0f, 1f)
        val tMapped = if (scheme == ColorScheme.INVERSE_RAINBOW) 1f - t else t
        val hue     = (270f - tMapped * 270f).let { h -> ((h % 360f) + 360f) % 360f }
        return hsvToColor(hue, saturation = 1f, value = 1f)
    }

    /** Convert HSV components to a fully-opaque Compose Color. */
    fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
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
