package com.chromasound.app.model

import androidx.compose.ui.graphics.Color

// ── User-adjustable settings ──────────────────────────────────────────────────

/**
 * All user-adjustable visualisation parameters in one place.
 * Held in a single StateFlow so the settings screen and ViewModel stay in sync.
 */
data class Settings(
    val bandCount:      Int   = BandDefinition.DEFAULT_BANDS,
    val lifetimeMs:     Long  = 500L,   // 100 ms – 2000 ms
    val circlesPerBand: Int   = 1,      // 1 – 5
    val minRadiusPx:    Float = 10f,    // 5 – 120 px
    val maxRadiusPx:    Float = 160f    // 20 – 250 px
) {
    companion object {
        const val MIN_LIFETIME_MS     = 100L
        const val MAX_LIFETIME_MS     = 2000L
        const val MIN_CIRCLES_PER_BAND = 1
        const val MAX_CIRCLES_PER_BAND = 5
        const val MIN_RADIUS_FLOOR    = 5f
        const val MAX_RADIUS_FLOOR    = 120f
        const val MIN_RADIUS_CEILING  = 20f
        const val MAX_RADIUS_CEILING  = 250f
    }
}

// ── Circle model ──────────────────────────────────────────────────────────────

/**
 * One frequency-band circle on the ChromaSound canvas.
 * Lifetime is supplied at spawn time from [Settings.lifetimeMs].
 */
data class FrequencyCircle(
    val bandIndex:    Int,
    val slotIndex:    Int,      // which circle slot within the band (0-based)
    val x:            Float,
    val y:            Float,
    val radiusPx:     Float,
    val color:        Color,
    val spawnTimeMs:  Long,
    val lifetimeMs:   Long,
    val centreHz:     Float,
    val decibelLevel: Float
) {
    fun lifeFraction(nowMs: Long): Float {
        val age = (nowMs - spawnTimeMs).coerceAtLeast(0L)
        return (1f - age.toFloat() / lifetimeMs).coerceIn(0f, 1f)
    }

    fun isAlive(nowMs: Long): Boolean = (nowMs - spawnTimeMs) < lifetimeMs
}

// ── Audio frame ───────────────────────────────────────────────────────────────

data class AudioFrame(
    val magnitudes:    FloatArray = FloatArray(0),
    val rmsVolume:     Float      = 0f,
    val decibelLevels: FloatArray = FloatArray(0),
    val bandPeakBins:  IntArray   = IntArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrame) return false
        return rmsVolume == other.rmsVolume
    }
    override fun hashCode(): Int = rmsVolume.hashCode()
}

// ── Band definition ───────────────────────────────────────────────────────────

data class BandDefinition(
    val count:    Int,
    val lowerHz:  FloatArray,
    val upperHz:  FloatArray,
    val centreHz: FloatArray
) {
    companion object {
        const val MIN_HZ       = 30f
        const val MAX_HZ       = 11_000f
        const val MIN_BANDS    = 2
        const val MAX_BANDS    = 24
        const val DEFAULT_BANDS = 16

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

    fun bandFor(hz: Float): Int {
        if (hz < MIN_HZ || hz > MAX_HZ) return -1
        for (i in 0 until count) { if (hz <= upperHz[i]) return i }
        return count - 1
    }

    override fun equals(other: Any?) = other is BandDefinition && count == other.count
    override fun hashCode()          = count
}
