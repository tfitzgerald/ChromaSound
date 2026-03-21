package com.chromasound.app.fft

import androidx.compose.ui.graphics.Color
import kotlin.math.*

/**
 * Maps FFT frequency bins → RGBA colors using a perceptual spectral mapping.
 *
 * Frequency → Hue mapping (mirrors visible light spectrum, inverted for aesthetics):
 *   Sub-bass   (20–60 Hz)    → Deep violet / indigo
 *   Bass       (60–250 Hz)   → Blue
 *   Low-mid    (250–500 Hz)  → Cyan / teal
 *   Mid        (500–2k Hz)   → Green → yellow
 *   High-mid   (2k–6k Hz)    → Orange → red
 *   Presence   (6k–12k Hz)   → Red → magenta
 *   Air/Treble (12k–22k Hz)  → Magenta → violet
 *
 * Magnitude (0..1) → Saturation and brightness.
 * Volume (RMS 0..1) → Overall blob radius / alpha scaling (handled by caller).
 */
object FrequencyColorMapper {

    /**
     * Convert a normalized frequency position [0, 1] to a hue angle [0°, 360°].
     * Uses a slightly curved mapping so mid-frequencies get more color range.
     */
    fun frequencyToHue(normalizedFreq: Float): Float {
        // Curve toward bass end — bass dominates sound so give it a wider arc
        val curved = normalizedFreq.pow(0.6f)
        // Map 0→1 to hue 260°→0° (violet → red, matching visible spectrum feel)
        return (1f - curved) * 260f
    }

    /**
     * Convert a single FFT bin into a Compose [Color].
     *
     * @param normalizedFreq   Bin position normalized to [0, 1]
     * @param magnitude        FFT magnitude normalized to [0, 1]
     * @param globalVolume     Overall RMS level [0, 1] for alpha modulation
     */
    fun binToColor(
        normalizedFreq: Float,
        magnitude: Float,
        globalVolume: Float
    ): Color {
        val hue = frequencyToHue(normalizedFreq)

        // Saturation: full at mid magnitudes, slightly desaturated at extremes
        val saturation = (0.5f + magnitude * 0.5f).coerceIn(0f, 1f)

        // Value/brightness: scales with magnitude — quiet = dark, loud = bright
        val value = (0.2f + magnitude * 0.8f).coerceIn(0f, 1f)

        // Alpha: magnitude * global volume — silent bins fade away completely
        val alpha = (magnitude * (0.3f + globalVolume * 0.7f)).coerceIn(0f, 1f)

        return hsvToColor(hue, saturation, value, alpha)
    }

    /** Convert HSV + alpha to Compose Color. */
    private fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float): Color {
        val h = hue / 60f
        val i = h.toInt()
        val f = h - i
        val p = value * (1f - saturation)
        val q = value * (1f - saturation * f)
        val t = value * (1f - saturation * (1f - f))

        val (r, g, b) = when (i % 6) {
            0 -> Triple(value, t, p)
            1 -> Triple(q, value, p)
            2 -> Triple(p, value, t)
            3 -> Triple(p, q, value)
            4 -> Triple(t, p, value)
            else -> Triple(value, p, q)
        }
        return Color(r, g, b, alpha)
    }
}

private fun Float.pow(exp: Float): Float = this.toDouble().pow(exp.toDouble()).toFloat()
