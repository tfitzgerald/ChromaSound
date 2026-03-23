package com.chromasound.app.model

import androidx.compose.ui.graphics.Color

/**
 * One frequency-band circle on the ChromaSound canvas.
 */
data class FrequencyCircle(
    val bandIndex: Int,
    val x: Float,
    val y: Float,
    val radiusPx: Float,
    val color: Color,
    val spawnTimeMs: Long,
    val centreHz: Float,
    val decibelLevel: Float
) {
    companion object {
        const val LIFETIME_MS = 500L
    }

    fun lifeFraction(nowMs: Long): Float {
        val age = (nowMs - spawnTimeMs).coerceAtLeast(0L)
        return (1f - age.toFloat() / LIFETIME_MS).coerceIn(0f, 1f)
    }

    fun isAlive(nowMs: Long): Boolean = (nowMs - spawnTimeMs) < LIFETIME_MS
}

/**
 * One analysed audio frame. [bandPeakBins] length matches the current band count.
 */
data class AudioFrame(
    val magnitudes: FloatArray = FloatArray(0),
    val rmsVolume: Float = 0f,
    val decibelLevels: FloatArray = FloatArray(0),
    val bandPeakBins: IntArray = IntArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrame) return false
        return rmsVolume == other.rmsVolume
    }
    override fun hashCode(): Int = rmsVolume.hashCode()
}

/**
 * Describes a set of logarithmically-spaced frequency bands.
 * Rebuilt whenever the user moves the band-count slider.
 *
 * Frequency range: 30 Hz – 11 000 Hz.
 * Band count:      2 – 24 (set by the settings slider).
 */
data class BandDefinition(
    val count: Int,
    val lowerHz: FloatArray,
    val upperHz: FloatArray,
    val centreHz: FloatArray
) {
    companion object {
        const val MIN_HZ   = 30f
        const val MAX_HZ   = 11_000f
        const val MIN_BANDS = 2
        const val MAX_BANDS = 24
        const val DEFAULT_BANDS = 16

        /** Build a [BandDefinition] for the requested [count]. */
        fun build(count: Int): BandDefinition {
            val n      = count.coerceIn(MIN_BANDS, MAX_BANDS)
            val lower  = FloatArray(n)
            val upper  = FloatArray(n)
            val centre = FloatArray(n)
            val logMin = Math.log10(MIN_HZ.toDouble())
            val logMax = Math.log10(MAX_HZ.toDouble())
            val step   = (logMax - logMin) / n
            for (i in 0 until n) {
                lower[i]  = Math.pow(10.0, logMin + i         * step).toFloat()
                upper[i]  = Math.pow(10.0, logMin + (i + 1)   * step).toFloat()
                centre[i] = Math.pow(10.0, logMin + (i + 0.5) * step).toFloat()
            }
            return BandDefinition(n, lower, upper, centre)
        }
    }

    /** Which band index does [hz] fall into? Returns -1 if out of range. */
    fun bandFor(hz: Float): Int {
        if (hz < MIN_HZ || hz > MAX_HZ) return -1
        for (i in 0 until count) {
            if (hz <= upperHz[i]) return i
        }
        return count - 1
    }

    override fun equals(other: Any?) = other is BandDefinition && count == other.count
    override fun hashCode()          = count
}
