package com.chromasound.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chromasound.app.audio.AudioCaptureEngine
import com.chromasound.app.audio.AudioCaptureEngine.Companion.DB_FLOOR
import com.chromasound.app.audio.AudioCaptureEngine.Companion.DB_SPAWN_THRESHOLD
import com.chromasound.app.audio.AudioCaptureEngine.Companion.FFT_SIZE
import com.chromasound.app.audio.AudioCaptureEngine.Companion.SAMPLE_RATE
import com.chromasound.app.fft.FrequencyColorMapper
import com.chromasound.app.model.AudioFrame
import com.chromasound.app.model.FrequencyCircle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

// ── UI State ─────────────────────────────────────────────────────────────────

sealed interface ChromaSoundUiState {
    data object Idle                : ChromaSoundUiState
    data object RequestingPermission: ChromaSoundUiState
    data object PermissionDenied    : ChromaSoundUiState
    data class Running(
        val circles: List<FrequencyCircle> = emptyList(),
        val rmsVolume: Float = 0f,
        val circleCount: Int = 0,
        val peakHz: String = "",
        val peakDb: String = ""
    ) : ChromaSoundUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ChromaSoundViewModel : ViewModel() {

    private val engine = AudioCaptureEngine()

    private val _uiState = MutableStateFlow<ChromaSoundUiState>(ChromaSoundUiState.Idle)
    val uiState: StateFlow<ChromaSoundUiState> = _uiState.asStateFlow()

    // All circles currently alive on screen
    private val activeCircles = mutableListOf<FrequencyCircle>()

    private var captureJob: Job? = null

    companion object {
        // FFT bin index → frequency in Hz
        // hz = binIndex * sampleRate / fftSize
        private fun binToHz(bin: Int): Float =
            bin.toFloat() * SAMPLE_RATE.toFloat() / FFT_SIZE.toFloat()

        // Radius in pixels driven by dB level.
        // DB_SPAWN_THRESHOLD (-45 dB) → MIN_RADIUS_PX
        // 0 dBFS (full scale) → MAX_RADIUS_PX
        private const val MIN_RADIUS_PX = 8f
        private const val MAX_RADIUS_PX = 180f

        fun dbToRadius(db: Float): Float {
            // Normalise dB within the spawnable range [DB_SPAWN_THRESHOLD .. 0]
            val range = 0f - DB_SPAWN_THRESHOLD          // = 45 dB of dynamic range
            val normalized = (db - DB_SPAWN_THRESHOLD) / range   // 0 = threshold, 1 = full scale
            return MIN_RADIUS_PX + normalized.coerceIn(0f, 1f) * (MAX_RADIUS_PX - MIN_RADIUS_PX)
        }

        // Frequency bin → stable X position on screen so each frequency
        // always appears in the same horizontal band.
        // Uses a golden-ratio hash to spread bins without clustering.
        private fun binToX(bin: Int): Float {
            val hash = abs((bin * 2654435761L).toInt()) // Knuth multiplicative hash
            return (hash % 1000) / 1000f
        }

        // Frequency → Y position: low frequencies at bottom, high at top.
        // Uses log scale so octaves are evenly spaced vertically.
        private fun hzToY(hz: Float): Float {
            val logMin = Math.log10(20.0)
            val logMax = Math.log10(22000.0)
            val logHz  = Math.log10(hz.toDouble().coerceIn(20.0, 22000.0))
            val t = ((logHz - logMin) / (logMax - logMin)).toFloat()
            return 1f - t   // invert: bass=bottom(1), treble=top(0)
        }

        private fun formatHz(hz: Float) = if (hz >= 1000f)
            "${"%.1f".format(hz / 1000f)} kHz" else "${hz.toInt()} Hz"
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun onPermissionGranted() {
        if (captureJob?.isActive == true) return
        _uiState.value = ChromaSoundUiState.Running()
        captureJob = viewModelScope.launch {
            engine.audioFrameFlow()
                .catch { _uiState.value = ChromaSoundUiState.Idle }
                .collect { frame -> processFrame(frame) }
        }
    }

    fun onPermissionDenied() {
        _uiState.value = ChromaSoundUiState.PermissionDenied
    }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        activeCircles.clear()
        _uiState.value = ChromaSoundUiState.Idle
    }

    fun resumeCapture() {
        _uiState.value = ChromaSoundUiState.RequestingPermission
    }

    // ── Frame processing ──────────────────────────────────────────────────────

    private fun processFrame(frame: AudioFrame) {
        val nowMs = System.currentTimeMillis()

        // 1. Remove circles that have exceeded their 1.5 s lifetime
        activeCircles.removeAll { !it.isAlive(nowMs) }

        // 2. Spawn one new circle per dominant frequency bin this frame.
        //    Each bin is a distinct detected frequency — color and radius are
        //    determined entirely by that bin's frequency (Hz) and dB level.
        for (bin in frame.dominantBins) {
            val hz = binToHz(bin)
            val db = if (bin < frame.decibelLevels.size) frame.decibelLevels[bin] else DB_FLOOR

            val circle = FrequencyCircle(
                x           = binToX(bin).coerceIn(0.05f, 0.95f),
                y           = hzToY(hz).coerceIn(0.05f, 0.95f),
                radiusPx    = dbToRadius(db),
                color       = FrequencyColorMapper.frequencyToColor(hz),
                spawnTimeMs = nowMs,
                frequencyHz = hz,
                decibelLevel= db
            )
            activeCircles.add(circle)
        }

        // 3. Build HUD info from the loudest bin
        val loudestBin = frame.dominantBins.firstOrNull()
        val peakHz  = loudestBin?.let { formatHz(binToHz(it)) } ?: "—"
        val peakDb  = loudestBin?.let {
            val d = if (it < frame.decibelLevels.size) frame.decibelLevels[it] else DB_FLOOR
            "${"%.1f".format(d)} dB"
        } ?: "—"

        _uiState.value = ChromaSoundUiState.Running(
            circles     = activeCircles.toList(),
            rmsVolume   = frame.rmsVolume,
            circleCount = activeCircles.size,
            peakHz      = peakHz,
            peakDb      = peakDb
        )
    }
}
