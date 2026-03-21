package com.chromasound.app.fft

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/**
 * Maps a frequency in Hz to a fully-opaque hue-saturated color.
 *
 * The mapping mirrors the visible light spectrum (inverted so low frequencies
 * feel warm-dark and high frequencies feel cool-bright):
 *
 *   Sub-bass   20 – 60 Hz    →  Deep violet   (hue ~270°)
 *   Bass       60 – 250 Hz   →  Blue          (hue ~230°)
 *   Low-mid   250 – 500 Hz   →  Cyan          (hue ~190°)
 *   Mid       500 – 2k  Hz   →  Green→Yellow  (hue ~120°–60°)
 *   High-mid    2k – 6k  Hz  →  Orange→Red    (hue ~30°–0°)
 *   Presence    6k – 12k Hz  →  Red→Magenta   (hue ~350°–310°)
 *   Air        12k – 22k Hz  →  Magenta→Violet(hue ~300°–270°)
 */
object FrequencyColorMapper {

    private const val MIN_HZ =   20f
    private const val MAX_HZ = 22000f

    /**
     * Convert a frequency in Hz to a Compose [Color].
     * Alpha is always 1f (fully opaque) — fading is handled by the canvas
     * using the circle's remaining lifetime fraction.
     */
    fun frequencyToColor(hz: Float): Color {
        // Logarithmic position in the audible spectrum [0, 1]
        val logMin = Math.log10(MIN_HZ.toDouble())
        val logMax = Math.log10(MAX_HZ.toDouble())
        val logHz  = Math.log10(hz.toDouble().coerceIn(MIN_HZ.toDouble(), MAX_HZ.toDouble()))
        val t = ((logHz - logMin) / (logMax - logMin)).toFloat().coerceIn(0f, 1f)

        // Map t [0=bass, 1=treble] → hue [270°=violet, 0°=red, wrapping through magenta]
        // We traverse: violet(270) → blue(230) → cyan(190) → green(120) → yellow(60) → red(0)
        // then for the top octave continue: red→magenta→violet
        val hue = when {
            t < 0.75f -> 270f - (t / 0.75f) * 270f   // violet → red over 75% of range
            else      -> (t - 0.75f) / 0.25f * (-30f) // red wraps back toward magenta
        }.let { h -> ((h % 360f) + 360f) % 360f }     // normalise to [0, 360)

        return hsvToColor(hue, saturation = 1f, value = 1f)
    }

    /**
     * Convert HSV to Compose [Color] (alpha always 1f).
     */
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
