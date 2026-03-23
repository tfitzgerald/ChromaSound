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

    // Use the app's actual frequency range so colours fill the whole wheel
    private const val MIN_HZ =   30f
    private const val MAX_HZ = 11_000f

    /**
     * Convert [hz] to a Compose [Color] using the given [scheme].
     * Alpha is always 1f — fading is done by the canvas draw loop.
     */
    fun frequencyToColor(hz: Float, scheme: ColorScheme = ColorScheme.RAINBOW): Color {
        // Logarithmic position in the frequency range [0 = bass, 1 = treble]
        val logMin = Math.log10(MIN_HZ.toDouble())
        val logMax = Math.log10(MAX_HZ.toDouble())
        val logHz  = Math.log10(hz.toDouble().coerceIn(MIN_HZ.toDouble(), MAX_HZ.toDouble()))
        val t = ((logHz - logMin) / (logMax - logMin)).toFloat().coerceIn(0f, 1f)

        // Apply scheme — invert t so bass and treble swap ends of the spectrum
        val tMapped = if (scheme == ColorScheme.INVERSE_RAINBOW) 1f - t else t

        // Hue traversal: violet (270°) → blue → cyan → green → yellow → red (0°)
        // tMapped = 0 → hue 270° (violet),  tMapped = 1 → hue 0° (red)
        // We spread evenly over 270° of the wheel (skipping the red-magenta-violet
        // wrap-around so each band colour stays maximally distinct).
        val hue = (270f - tMapped * 270f).let { h -> ((h % 360f) + 360f) % 360f }

        return hsvToColor(hue, saturation = 1f, value = 1f)
    }

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
