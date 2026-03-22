package com.chromasound.app.model

import androidx.compose.ui.graphics.Color

/**
 * One frequency-band circle on the ChromaSound canvas.
 *
 * Rules:
 *  - Color      → hue mapped from the band's centre frequency
 *  - Radius     → decibel level of the dominant bin inside the band
 *  - Lifetime   → exactly 500 ms, then removed
 *  - One circle per band maximum — newer spawn replaces older
 *
 * @param bandIndex    Which of the 16 frequency bands this belongs to (0 = lowest)
 * @param x            Normalised horizontal position [0, 1]
 * @param y            Normalised vertical position [0, 1]
 * @param radiusPx     Radius in pixels, driven by dB level
 * @param color        Fully-opaque hue from the band's centre frequency
 * @param spawnTimeMs  System.currentTimeMillis() at birth
 * @param centreHz     Centre frequency of this band (for HUD display)
 * @param decibelLevel dB level that set the radius (for HUD display)
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
        const val LIFETIME_MS = 500L    // 0.5 seconds
    }

    /** Remaining life fraction: 1.0 = just born, 0.0 = expired */
    fun lifeFraction(nowMs: Long): Float {
        val age = (nowMs - spawnTimeMs).coerceAtLeast(0L)
        return (1f - age.toFloat() / LIFETIME_MS).coerceIn(0f, 1f)
    }

    fun isAlive(nowMs: Long): Boolean = (nowMs - spawnTimeMs) < LIFETIME_MS
}

/**
 * One analysed audio frame from the FFT pipeline.
 *
 * @param magnitudes      Normalised FFT magnitudes, length = FFT_SIZE / 2
 * @param rmsVolume       Root-mean-square amplitude [0, 1]
 * @param decibelLevels   Per-bin dBFS value (DB_FLOOR … 0)
 * @param bandPeakBins    For each of the 16 bands: the bin index with the
 *                        highest dB in that band, or -1 if the band is silent
 */
data class AudioFrame(
    val magnitudes: FloatArray = FloatArray(0),
    val rmsVolume: Float = 0f,
    val decibelLevels: FloatArray = FloatArray(0),
    val bandPeakBins: IntArray = IntArray(FrequencyBands.COUNT) { -1 }
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrame) return false
        return rmsVolume == other.rmsVolume
    }
    override fun hashCode(): Int = rmsVolume.hashCode()
}

/**
 * Definition of the 16 logarithmically-spaced frequency bands
 * covering 30 Hz – 12 000 Hz.
 *
 * Logarithmic spacing gives equal perceptual width to each band
 * (same number of octave-fractions per band).
 */
object FrequencyBands {
    const val COUNT    = 16
    const val MIN_HZ   = 30f
    const val MAX_HZ   = 12_000f

    /** Lower edge of each band in Hz (size = COUNT) */
    val lowerHz: FloatArray
    /** Upper edge of each band in Hz (size = COUNT) */
    val upperHz: FloatArray
    /** Centre frequency of each band in Hz (size = COUNT) */
    val centreHz: FloatArray

    init {
        lowerHz  = FloatArray(COUNT)
        upperHz  = FloatArray(COUNT)
        centreHz = FloatArray(COUNT)

        val logMin = Math.log10(MIN_HZ.toDouble())
        val logMax = Math.log10(MAX_HZ.toDouble())
        val step   = (logMax - logMin) / COUNT

        for (i in 0 until COUNT) {
            lowerHz[i]  = Math.pow(10.0, logMin + i       * step).toFloat()
            upperHz[i]  = Math.pow(10.0, logMin + (i + 1) * step).toFloat()
            centreHz[i] = Math.pow(10.0, logMin + (i + 0.5) * step).toFloat()
        }
    }

    /** Which band index (0-based) does a frequency in Hz belong to? Returns -1 if out of range. */
    fun bandFor(hz: Float): Int {
        if (hz < MIN_HZ || hz > MAX_HZ) return -1
        for (i in 0 until COUNT) {
            if (hz <= upperHz[i]) return i
        }
        return COUNT - 1
    }
}
