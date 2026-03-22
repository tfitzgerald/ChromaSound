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
import com.chromasound.app.model.FrequencyBands
import com.chromasound.app.model.FrequencyCircle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

sealed interface ChromaSoundUiState {
    data object Idle                : ChromaSoundUiState
    data object RequestingPermission: ChromaSoundUiState
    data object PermissionDenied    : ChromaSoundUiState
    data class Running(
        val circles: List<FrequencyCircle> = emptyList(),
        val rmsVolume: Float = 0f,
        val activeCount: Int = 0,     // how many bands are currently lit
        val peakHz: String = "",
        val peakDb: String = ""
    ) : ChromaSoundUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ChromaSoundViewModel : ViewModel() {

    private val engine = AudioCaptureEngine()

    private val _uiState = MutableStateFlow<ChromaSoundUiState>(ChromaSoundUiState.Idle)
    val uiState: StateFlow<ChromaSoundUiState> = _uiState.asStateFlow()

    /**
     * One slot per frequency band — at most 1 circle alive per band at any time.
     * Index = band index (0 = lowest frequency band).
     */
    private val bandCircle = arrayOfNulls<FrequencyCircle>(FrequencyBands.COUNT)

    private var captureJob: Job? = null

    companion object {
        // Radius range in pixels
        private const val MIN_RADIUS_PX = 10f
        private const val MAX_RADIUS_PX = 160f

        /** dBFS → radius: threshold = min, 0 dBFS = max */
        fun dbToRadius(db: Float): Float {
            val range      = 0f - DB_SPAWN_THRESHOLD          // e.g. 50 dB
            val normalized = (db - DB_SPAWN_THRESHOLD) / range
            return (MIN_RADIUS_PX + normalized.coerceIn(0f, 1f) * (MAX_RADIUS_PX - MIN_RADIUS_PX))
        }

        /** FFT bin index → frequency in Hz */
        fun binToHz(bin: Int): Float =
            bin.toFloat() * SAMPLE_RATE.toFloat() / FFT_SIZE.toFloat()

        fun formatHz(hz: Float) =
            if (hz >= 1000f) "${"%.1f".format(hz / 1000f)} kHz" else "${hz.toInt()} Hz"
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
        bandCircle.fill(null)
        _uiState.value = ChromaSoundUiState.Idle
    }

    fun resumeCapture() {
        _uiState.value = ChromaSoundUiState.RequestingPermission
    }

    // ── Frame processing ──────────────────────────────────────────────────────

    private fun processFrame(frame: AudioFrame) {
        val nowMs = System.currentTimeMillis()

        // 1. Expire dead circles (older than 500 ms)
        for (b in 0 until FrequencyBands.COUNT) {
            val c = bandCircle[b]
            if (c != null && !c.isAlive(nowMs)) bandCircle[b] = null
        }

        // 2. For each band: if the engine found a peak bin, spawn/replace circle.
        //    One circle per band — the new one always wins (it's more current).
        for (band in 0 until FrequencyBands.COUNT) {
            val peakBin = frame.bandPeakBins[band]
            if (peakBin < 0) continue                         // band is silent this frame

            val db         = frame.decibelLevels.getOrElse(peakBin) { DB_FLOOR }
            val centreHz   = FrequencyBands.centreHz[band]
            val color      = FrequencyColorMapper.frequencyToColor(centreHz)
            val radiusPx   = dbToRadius(db)

            // X: evenly distributed — band 0 at left edge, band 15 at right edge
            val x = (band + 0.5f) / FrequencyBands.COUNT

            // Y: fixed at vertical centre (±small offset per band for visual variety)
            // Odd bands sit slightly above centre, even bands slightly below
            val y = if (band % 2 == 0) 0.55f else 0.45f

            bandCircle[band] = FrequencyCircle(
                bandIndex    = band,
                x            = x,
                y            = y,
                radiusPx     = radiusPx,
                color        = color,
                spawnTimeMs  = nowMs,
                centreHz     = centreHz,
                decibelLevel = db
            )
        }

        // 3. Build snapshot for the UI
        val alive = bandCircle.filterNotNull()

        // Loudest active band for HUD
        val loudest = alive.maxByOrNull { it.decibelLevel }

        _uiState.value = ChromaSoundUiState.Running(
            circles     = alive,
            rmsVolume   = frame.rmsVolume,
            activeCount = alive.size,
            peakHz      = loudest?.let { formatHz(it.centreHz) } ?: "—",
            peakDb      = loudest?.let { "${"%.1f".format(it.decibelLevel)} dB" } ?: "—"
        )
    }
}
