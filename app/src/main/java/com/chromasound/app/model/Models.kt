package com.chromasound.app.model

import androidx.compose.ui.graphics.Color

// ── Color scheme enum ─────────────────────────────────────────────────────────

enum class ColorScheme {
    RAINBOW,
    INVERSE_RAINBOW
}

// ── Object shape enum ─────────────────────────────────────────────────────────

enum class ObjectShape {
    CIRCLE, STAR, BOX_2D, BOX_3D, SPHERE,
    FRACTAL_KOCH,
    FRACTAL_SIERPINSKI,
    FRACTAL_DRAGON,
    VECTOR,         // spinning frequency vector — length=dB, colour=freq, 15s lifetime
    RIBBON          // flowing silk ribbon wave — amplitude=dB, ripple=freq band
}

// ── Mirror mode enum ──────────────────────────────────────────────────────────

enum class MirrorMode {
    OFF, HORIZONTAL, VERTICAL, QUAD
}

// ── Background effect enum ────────────────────────────────────────────────────

enum class BackgroundEffect {
    NONE,
    STARFIELD,
    BLOOM,
    NOISE,
    JULIA,       // Julia set fractal — seed driven by dominant frequency + RMS
    TERRAIN      // 3D perspective frequency terrain — scrolls toward viewer
}

// ── Theme mode enum ───────────────────────────────────────────────────────────

enum class ThemeMode {
    DARK,   // always dark (default)
    LIGHT,  // always light
    SYSTEM  // follows device system setting
}

// ── Settings ──────────────────────────────────────────────────────────────────

data class Settings(
    val bandCount:      Int              = BandDefinition.DEFAULT_BANDS,
    val lifetimeMs:     Long             = 500L,
    val circlesPerBand: Int              = 1,
    val minRadiusPx:    Float            = 10f,
    val maxRadiusPx:    Float            = 160f,
    val placement:      Float            = 0.3f,
    val sensitivity:    Float            = 1.0f,
    val colorScheme:    ColorScheme      = ColorScheme.RAINBOW,
    val objectShape:    ObjectShape      = ObjectShape.CIRCLE,
    val subBands:       Int              = 4,
    val bandColors:     Map<Int, Color>  = emptyMap(),
    val noiseGateDb:    Float            = -50f,
    val mirrorMode:     MirrorMode       = MirrorMode.OFF,
    val trailLength:    Int              = 0,
    val beatSensitivity: Float          = 1.3f,
    val colorAnimSpeed:  Float          = 0f,
    val showWaveform:    Boolean         = false,
    // Particle explosions — burst of particles on loud transients
    val particlesEnabled:   Boolean     = false,
    val particleThreshold:  Float       = 0.6f,
    val oscilloscopeMode:   Boolean     = false,
    val backgroundEffect:   BackgroundEffect = BackgroundEffect.NONE,
    val themeMode:          ThemeMode   = ThemeMode.DARK,
    // Auto-Gain Control — normalises quiet/loud input automatically
    val autoGain:           Boolean     = true,
    // Shape opacity multiplier — 0.3 (ghostly) to 1.0 (full)
    val shapeOpacity:       Float       = 1.0f
) {
    companion object {
        const val MIN_LIFETIME_MS           = 100L
        const val MAX_LIFETIME_MS           = 2000L
        const val MIN_CIRCLES_PER_BAND      = 1
        const val MAX_CIRCLES_PER_BAND      = 5
        const val MIN_RADIUS_FLOOR          = 5f
        const val MAX_RADIUS_FLOOR          = 120f
        const val MIN_RADIUS_CEILING        = 20f
        const val MAX_RADIUS_CEILING        = 250f
        const val MIN_PLACEMENT             = 0f
        const val MAX_PLACEMENT             = 1f
        const val MIN_SENSITIVITY           = 0.1f
        const val MAX_SENSITIVITY           = 3.0f
        const val MIN_SUB_BANDS             = 1
        const val MAX_SUB_BANDS             = 12
        const val MIN_NOISE_GATE_DB         = -70f
        const val MAX_NOISE_GATE_DB         = -20f
        const val DEFAULT_NOISE_GATE_DB     = -50f
        const val MIN_TRAIL_LENGTH          = 0
        const val MAX_TRAIL_LENGTH          = 8
        const val MIN_BEAT_SENSITIVITY      = 1.1f
        const val MAX_BEAT_SENSITIVITY      = 2.5f
        const val DEFAULT_BEAT_SENSITIVITY  = 1.3f
        const val MIN_COLOR_ANIM_SPEED      = 0f
        const val MAX_COLOR_ANIM_SPEED      = 3f
        const val MIN_PARTICLE_THRESHOLD    = 0.1f
        const val MAX_PARTICLE_THRESHOLD    = 1.0f
        const val MIN_SHAPE_OPACITY         = 0.2f
        const val MAX_SHAPE_OPACITY         = 1.0f
    }
}

// ── FrequencyCircle ───────────────────────────────────────────────────────────

/**
 * @param subBandEnergies  Normalised energy (0–1) for each sub-band slice within
 *                         this band, ordered lowest→highest frequency (= centre→edge).
 *                         Length equals [Settings.subBands] at spawn time.
 *                         Used to shade each radial ring independently.
 */
data class FrequencyCircle(
    val bandIndex:       Int,
    val slotIndex:       Int,
    val x:               Float,
    val y:               Float,
    val radiusPx:        Float,
    val color:           Color,
    val spawnTimeMs:     Long,
    val lifetimeMs:      Long,
    val centreHz:        Float,
    val decibelLevel:    Float,
    val subBandEnergies: FloatArray = FloatArray(1) { 1f }
) {
    fun lifeFraction(nowMs: Long): Float {
        val age = (nowMs - spawnTimeMs).coerceAtLeast(0L)
        return (1f - age.toFloat() / lifetimeMs).coerceIn(0f, 1f)
    }
    fun isAlive(nowMs: Long): Boolean = (nowMs - spawnTimeMs) < lifetimeMs

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FrequencyCircle) return false
        return bandIndex == other.bandIndex && slotIndex == other.slotIndex &&
               spawnTimeMs == other.spawnTimeMs
    }
    override fun hashCode(): Int = 31 * (31 * bandIndex + slotIndex) + spawnTimeMs.toInt()
}

// ── AudioFrame ────────────────────────────────────────────────────────────────

/**
 * @param bandSubEnergies  [bandIndex][subBandIndex] → normalised energy (0–1).
 *                         Outer array length = band count; inner = sub-band count.
 */
data class AudioFrame(
    val magnitudes:       FloatArray        = FloatArray(0),
    val rmsVolume:        Float             = 0f,
    val decibelLevels:    FloatArray        = FloatArray(0),
    val bandPeakBins:     IntArray          = IntArray(0),
    val bandSubEnergies:  Array<FloatArray> = emptyArray(),
    val isBeat:           Boolean           = false,
    val bpm:              Float             = 0f,
    // Downsampled PCM waveform for overlay — 256 samples normalised to [-1, 1]
    val waveformSamples:  FloatArray        = FloatArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrame) return false
        return rmsVolume == other.rmsVolume
    }
    override fun hashCode(): Int = rmsVolume.hashCode()
}

// ── BandDefinition ────────────────────────────────────────────────────────────

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
