package com.chromasound.app.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chromasound.app.audio.AudioCaptureEngine
import com.chromasound.app.audio.AudioCaptureEngine.Companion.FFT_SIZE
import com.chromasound.app.fft.FrequencyColorMapper
import com.chromasound.app.model.AudioFrame
import com.chromasound.app.model.ColorBlotch
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

// ── UI State ────────────────────────────────────────────────────────────────

sealed interface ChromaSoundUiState {
    data object Idle : ChromaSoundUiState
    data object RequestingPermission : ChromaSoundUiState
    data object PermissionDenied : ChromaSoundUiState
    data class Running(
        val blotches: List<ColorBlotch> = emptyList(),
        val rmsVolume: Float = 0f,
        val peakFrequencyLabel: String = ""
    ) : ChromaSoundUiState
}

// ── ViewModel ───────────────────────────────────────────────────────────────

class ChromaSoundViewModel : ViewModel() {

    private val engine = AudioCaptureEngine()

    private val _uiState = MutableStateFlow<ChromaSoundUiState>(ChromaSoundUiState.Idle)
    val uiState: StateFlow<ChromaSoundUiState> = _uiState.asStateFlow()

    /** Active blotches on screen — decayed each frame and augmented with new spawns. */
    private val activeBlotches = ArrayDeque<ColorBlotch>(MAX_BLOTCHES * 2)

    private var captureJob: Job? = null

    companion object {
        const val MAX_BLOTCHES = 80          // Hard cap to keep GPU load reasonable
        const val BLOTCH_DECAY_BASE = 0.008f // Lifetime drains per frame
        const val BINS_HALF = FFT_SIZE / 2
    }

    // ── Public API ───────────────────────────────────────────────────────────

    fun onPermissionGranted() {
        if (captureJob?.isActive == true) return
        _uiState.value = ChromaSoundUiState.Running()

        captureJob = viewModelScope.launch {
            engine.audioFrameFlow()
                .catch { e ->
                    // Surface any AudioRecord errors to the UI
                    _uiState.value = ChromaSoundUiState.Idle
                }
                .collect { frame -> processFrame(frame) }
        }
    }

    fun onPermissionDenied() {
        _uiState.value = ChromaSoundUiState.PermissionDenied
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        activeBlotches.clear()
        _uiState.value = ChromaSoundUiState.Idle
    }

    fun resumeCapture() {
        _uiState.value = ChromaSoundUiState.RequestingPermission
    }

    // ── Frame processing ─────────────────────────────────────────────────────

    private fun processFrame(frame: AudioFrame) {
        // 1. Decay all existing blotches
        val decayed = activeBlotches.map { it.decayed() }.filter { it.isAlive }
        activeBlotches.clear()
        activeBlotches.addAll(decayed)

        // 2. Spawn new blotches from dominant frequency bins
        val newBlotches = frame.dominantBins.map { bin ->
            spawnBlotch(bin, frame)
        }

        // 3. Add new blotches, trimming oldest if over cap
        activeBlotches.addAll(newBlotches)
        while (activeBlotches.size > MAX_BLOTCHES) activeBlotches.removeFirst()

        // 4. Determine the peak frequency label for display
        val peakBin = frame.dominantBins.firstOrNull()
        val peakHz = peakBin?.let { binToHz(it) }
        val peakLabel = peakHz?.let { formatHz(it) } ?: ""

        _uiState.value = ChromaSoundUiState.Running(
            blotches = activeBlotches.toList(),
            rmsVolume = frame.rmsVolume,
            peakFrequencyLabel = peakLabel
        )
    }

    /**
     * Convert an FFT bin index to Hz.
     * hz = bin * sampleRate / fftSize
     */
    private fun binToHz(bin: Int): Float =
        bin.toFloat() * AudioCaptureEngine.SAMPLE_RATE / FFT_SIZE.toFloat()

    private fun formatHz(hz: Float): String = when {
        hz >= 1000f -> "${"%.1f".format(hz / 1000f)} kHz"
        else        -> "${hz.toInt()} Hz"
    }

    /**
     * Spawn a [ColorBlotch] from a dominant FFT bin.
     *
     * Position is seeded by the bin index so the same frequency consistently
     * appears in the same region — bass at the bottom, treble at the top —
     * with jitter to create organic overlap.
     */
    private fun spawnBlotch(bin: Int, frame: AudioFrame): ColorBlotch {
        val magnitude = if (bin < frame.magnitudes.size) frame.magnitudes[bin] else 0f
        val normalizedFreq = bin.toFloat() / BINS_HALF

        // Vertical position: low frequency → bottom, high → top (with jitter)
        val jitterY = Random.nextFloat() * 0.25f - 0.125f
        val y = (1f - normalizedFreq + jitterY).coerceIn(0.05f, 0.95f)

        // Horizontal position: pseudo-random per bin, slightly drifting
        val xSeed = abs(bin * 1618033 % 1000) / 1000f          // Golden-ratio hash
        val jitterX = Random.nextFloat() * 0.3f - 0.15f
        val x = (xSeed + jitterX).coerceIn(0.05f, 0.95f)

        // Radius: volume × magnitude — loud + strong bin = large blotch
        val radius = (0.04f + frame.rmsVolume * magnitude * 0.35f).coerceIn(0.02f, 0.45f)

        // Decay faster for smaller, quieter blotches
        val decayRate = BLOTCH_DECAY_BASE + (1f - magnitude) * 0.01f

        val color = FrequencyColorMapper.binToColor(normalizedFreq, magnitude, frame.rmsVolume)

        return ColorBlotch(
            x = x, y = y,
            radius = radius,
            color = color,
            life = 1f,
            decayRate = decayRate,
            frequencyBin = bin
        )
    }
}
