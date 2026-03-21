package com.chromasound.app.model

import androidx.compose.ui.graphics.Color

/**
 * A single frequency circle displayed on the ChromaSound canvas.
 *
 * Rules:
 *  - Color    → derived from the frequency (Hz) of the FFT bin
 *  - Radius   → derived from the decibel level of that bin
 *  - Lifetime → exactly 1.5 seconds, then removed
 *
 * @param x              Normalised horizontal position [0, 1]
 * @param y              Normalised vertical position [0, 1]
 * @param radiusPx       Radius in pixels (computed from dB level)
 * @param color          Fully-opaque hue mapped from frequency
 * @param spawnTimeMs    System.currentTimeMillis() when this circle was born
 * @param frequencyHz    The actual frequency in Hz (for HUD display)
 * @param decibelLevel   The dB level that set this circle's radius (for HUD display)
 */
data class FrequencyCircle(
    val x: Float,
    val y: Float,
    val radiusPx: Float,
    val color: Color,
    val spawnTimeMs: Long,
    val frequencyHz: Float,
    val decibelLevel: Float
) {
    companion object {
        const val LIFETIME_MS = 1500L   // 1.5 seconds exactly
    }

    /** Fraction of life remaining: 1.0 = just spawned, 0.0 = expired */
    fun lifefraction(nowMs: Long): Float {
        val age = (nowMs - spawnTimeMs).coerceAtLeast(0L)
        return (1f - age.toFloat() / LIFETIME_MS).coerceIn(0f, 1f)
    }

    fun isAlive(nowMs: Long): Boolean = (nowMs - spawnTimeMs) < LIFETIME_MS
}

/**
 * One analysed audio frame from the FFT pipeline.
 *
 * @param magnitudes    Normalised FFT magnitudes, length = FFT_SIZE / 2
 * @param rmsVolume     Root-mean-square amplitude [0, 1]
 * @param decibelLevels Per-bin dB value (negative, e.g. -60 dB = silence, 0 dB = full scale)
 * @param dominantBins  Indices of the loudest bins above the silence threshold
 */
data class AudioFrame(
    val magnitudes: FloatArray = FloatArray(0),
    val rmsVolume: Float = 0f,
    val decibelLevels: FloatArray = FloatArray(0),
    val dominantBins: List<Int> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrame) return false
        return rmsVolume == other.rmsVolume && dominantBins == other.dominantBins
    }
    override fun hashCode(): Int = 31 * rmsVolume.hashCode() + dominantBins.hashCode()
}
