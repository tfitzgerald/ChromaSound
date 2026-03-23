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
import com.chromasound.app.model.BandDefinition
import com.chromasound.app.model.FrequencyCircle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

sealed interface ChromaSoundUiState {
    data object Idle                 : ChromaSoundUiState
    data object RequestingPermission : ChromaSoundUiState
    data object PermissionDenied     : ChromaSoundUiState
    data class Running(
        val circles: List<FrequencyCircle> = emptyList(),
        val rmsVolume: Float = 0f,
        val activeCount: Int = 0,
        val bandCount: Int = BandDefinition.DEFAULT_BANDS,
        val peakHz: String = "",
        val peakDb: String = ""
    ) : ChromaSoundUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ChromaSoundViewModel : ViewModel() {

    private val engine = AudioCaptureEngine()

    private val _uiState = MutableStateFlow<ChromaSoundUiState>(ChromaSoundUiState.Idle)
    val uiState: StateFlow<ChromaSoundUiState> = _uiState.asStateFlow()

    // Current band count — survives navigation to/from settings
    private val _bandCount = MutableStateFlow(BandDefinition.DEFAULT_BANDS)
    val bandCount: StateFlow<Int> = _bandCount.asStateFlow()

    // Rebuilt whenever bandCount changes; read by the audio coroutine
    @Volatile private var currentBands = BandDefinition.build(BandDefinition.DEFAULT_BANDS)

    // One circle slot per band — resized when band count changes
    private var bandCircle = arrayOfNulls<FrequencyCircle>(BandDefinition.DEFAULT_BANDS)

    private var captureJob: Job? = null

    companion object {
        private const val MIN_RADIUS_PX = 10f
        private const val MAX_RADIUS_PX = 160f

        fun dbToRadius(db: Float): Float {
            val range      = 0f - DB_SPAWN_THRESHOLD
            val normalized = (db - DB_SPAWN_THRESHOLD) / range
            return MIN_RADIUS_PX + normalized.coerceIn(0f, 1f) * (MAX_RADIUS_PX - MIN_RADIUS_PX)
        }

        fun formatHz(hz: Float) =
            if (hz >= 1000f) "${"%.1f".format(hz / 1000f)} kHz" else "${hz.toInt()} Hz"
    }

    // ── Settings API ──────────────────────────────────────────────────────────

    fun setBandCount(count: Int) {
        val clamped = count.coerceIn(BandDefinition.MIN_BANDS, BandDefinition.MAX_BANDS)
        if (clamped == _bandCount.value) return
        _bandCount.value = clamped
        currentBands     = BandDefinition.build(clamped)
        // Resize the slot array — old circles are discarded (they'd be in wrong slots)
        bandCircle       = arrayOfNulls(clamped)
    }

    // ── Capture lifecycle ─────────────────────────────────────────────────────

    fun onPermissionGranted() {
        if (captureJob?.isActive == true) return
        _uiState.value = ChromaSoundUiState.Running(bandCount = _bandCount.value)
        captureJob = viewModelScope.launch {
            engine.audioFrameFlow { currentBands }
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
        val bd    = currentBands

        // Resize slot array if band count changed between frames
        if (bandCircle.size != bd.count) bandCircle = arrayOfNulls(bd.count)

        // Expire dead circles
        for (b in 0 until bd.count) {
            val c = bandCircle[b]
            if (c != null && !c.isAlive(nowMs)) bandCircle[b] = null
        }

        // Spawn one circle per active band
        val peakBins = frame.bandPeakBins
        for (band in 0 until minOf(bd.count, peakBins.size)) {
            val peakBin = peakBins[band]
            if (peakBin < 0) continue

            val db       = frame.decibelLevels.getOrElse(peakBin) { DB_FLOOR }
            val centreHz = bd.centreHz[band]

            bandCircle[band] = FrequencyCircle(
                bandIndex    = band,
                x            = (band + 0.5f) / bd.count,
                y            = if (band % 2 == 0) 0.55f else 0.45f,
                radiusPx     = dbToRadius(db),
                color        = FrequencyColorMapper.frequencyToColor(centreHz),
                spawnTimeMs  = nowMs,
                centreHz     = centreHz,
                decibelLevel = db
            )
        }

        val alive   = bandCircle.filterNotNull()
        val loudest = alive.maxByOrNull { it.decibelLevel }

        _uiState.value = ChromaSoundUiState.Running(
            circles     = alive,
            rmsVolume   = frame.rmsVolume,
            activeCount = alive.size,
            bandCount   = bd.count,
            peakHz      = loudest?.let { formatHz(it.centreHz) } ?: "—",
            peakDb      = loudest?.let { "${"%.1f".format(it.decibelLevel)} dB" } ?: "—"
        )
    }
}
