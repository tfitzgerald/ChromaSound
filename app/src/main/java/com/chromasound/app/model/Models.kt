package com.chromasound.app.model

import androidx.compose.ui.graphics.Color

// ── Color scheme enum ─────────────────────────────────────────────────────────

enum class ColorScheme {
    /** Bass = violet → treble = red  (matches visible light spectrum) */
    RAINBOW,
    /** Bass = red → treble = violet  (inverted spectrum) */
    INVERSE_RAINBOW
}

// ── User-adjustable settings ──────────────────────────────────────────────────

data class Settings(
    val bandCount:      Int         = BandDefinition.DEFAULT_BANDS,
    val lifetimeMs:     Long        = 500L,
    val circlesPerBand: Int         = 1,
    val minRadiusPx:    Float       = 10f,
    val maxRadiusPx:    Float       = 160f,
    // 0.0 = every circle locked to band centre column
    // 1.0 = circles scatter freely across the full screen width
    val placement:      Float       = 0.3f,
    // Multiplier applied to raw dB values before threshold comparison.
    // < 1.0 = less sensitive (only loud sounds trigger circles)
    // > 1.0 = more sensitive (quiet sounds also trigger circles)
    val sensitivity:    Float       = 1.0f,
    val colorScheme:    ColorScheme = ColorScheme.RAINBOW
) {
    companion object {
        const val MIN_LIFETIME_MS      = 100L
        const val MAX_LIFETIME_MS      = 2000L
        const val MIN_CIRCLES_PER_BAND = 1
        const val MAX_CIRCLES_PER_BAND = 5
        const val MIN_RADIUS_FLOOR     = 5f
        const val MAX_RADIUS_FLOOR     = 120f
        const val MIN_RADIUS_CEILING   = 20f
        const val MAX_RADIUS_CEILING   = 250f
        const val MIN_PLACEMENT        = 0f
        const val MAX_PLACEMENT        = 1f
        const val MIN_SENSITIVITY      = 0.1f
        const val MAX_SENSITIVITY      = 3.0f
    }
}

// ── Circle model ──────────────────────────────────────────────────────────────

data class FrequencyCircle(
    val bandIndex:    Int,
    val slotIndex:    Int,
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
        const val MIN_HZ        = 30f
        const val MAX_HZ        = 11_000f
        const val MIN_BANDS     = 2
        const val MAX_BANDS     = 24
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
