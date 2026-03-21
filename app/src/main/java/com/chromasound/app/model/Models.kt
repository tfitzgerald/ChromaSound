package com.chromasound.app.model

import androidx.compose.ui.graphics.Color

/**
 * Represents a single rendered color blotch on the ChromaSound canvas.
 *
 * Blotches are spawned from dominant FFT frequency bins and decay over time.
 *
 * @param x            Normalized horizontal position [0, 1]
 * @param y            Normalized vertical position [0, 1]
 * @param radius       Normalized radius [0, 1] relative to screen width
 * @param color        RGBA color derived from frequency and magnitude
 * @param life         Current life [0, 1] — 1 = freshly spawned, 0 = dead
 * @param decayRate    How fast life drains per frame [0, 1]
 * @param frequencyBin Source FFT bin index (used for position seeding)
 */
data class ColorBlotch(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val life: Float = 1f,
    val decayRate: Float = 0.015f,
    val frequencyBin: Int = 0
) {
    val isAlive: Boolean get() = life > 0.01f

    fun decayed(): ColorBlotch = copy(life = (life - decayRate).coerceAtLeast(0f))
}

/**
 * Snapshot of one analysis frame passed from the audio engine to the UI.
 *
 * @param magnitudes    Normalized FFT magnitudes (size = FFT_SIZE / 2)
 * @param rmsVolume     Root-mean-square volume [0, 1]
 * @param dominantBins  Indices of the top N loudest frequency bins
 */
data class AudioFrame(
    val magnitudes: FloatArray = FloatArray(0),
    val rmsVolume: Float = 0f,
    val dominantBins: List<Int> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrame) return false
        return rmsVolume == other.rmsVolume && dominantBins == other.dominantBins
    }

    override fun hashCode(): Int = 31 * rmsVolume.hashCode() + dominantBins.hashCode()
}
