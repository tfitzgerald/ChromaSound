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
import com.chromasound.app.model.Settings
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
        val circles:     List<FrequencyCircle> = emptyList(),
        val rmsVolume:   Float = 0f,
        val activeCount: Int   = 0,
        val bandCount:   Int   = BandDefinition.DEFAULT_BANDS,
        val peakHz:      String = "",
        val peakDb:      String = ""
    ) : ChromaSoundUiState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ChromaSoundViewModel : ViewModel() {

    private val engine = AudioCaptureEngine()

    private val _uiState = MutableStateFlow<ChromaSoundUiState>(ChromaSoundUiState.Idle)
    val uiState: StateFlow<ChromaSoundUiState> = _uiState.asStateFlow()

    // Single source of truth for all user settings
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    // Derived band definition — rebuilt whenever bandCount changes
    @Volatile private var currentBands = BandDefinition.build(BandDefinition.DEFAULT_BANDS)

    // Circle slots: [band][slot] — outer array resizes with bandCount,
    // inner array resizes with circlesPerBand
    @Volatile private var bandSlots =
        Array(BandDefinition.DEFAULT_BANDS) { arrayOfNulls<FrequencyCircle>(1) }

    private var captureJob: Job? = null

    companion object {
        fun binToHz(bin: Int): Float =
            bin.toFloat() * SAMPLE_RATE.toFloat() / FFT_SIZE.toFloat()

        fun formatHz(hz: Float) =
            if (hz >= 1000f) "${"%.1f".format(hz / 1000f)} kHz" else "${hz.toInt()} Hz"

        /** Map a dB level to a pixel radius using the current min/max settings. */
        fun dbToRadius(db: Float, minPx: Float, maxPx: Float): Float {
            val range      = 0f - DB_SPAWN_THRESHOLD          // e.g. 50 dB of usable range
            val normalized = (db - DB_SPAWN_THRESHOLD) / range
            return (minPx + normalized.coerceIn(0f, 1f) * (maxPx - minPx))
        }
    }

    // ── Settings API ──────────────────────────────────────────────────────────

    fun updateSettings(newSettings: Settings) {
        val s = newSettings.copy(
            bandCount      = newSettings.bandCount.coerceIn(BandDefinition.MIN_BANDS, BandDefinition.MAX_BANDS),
            lifetimeMs     = newSettings.lifetimeMs.coerceIn(Settings.MIN_LIFETIME_MS, Settings.MAX_LIFETIME_MS),
            circlesPerBand = newSettings.circlesPerBand.coerceIn(Settings.MIN_CIRCLES_PER_BAND, Settings.MAX_CIRCLES_PER_BAND),
            minRadiusPx    = newSettings.minRadiusPx.coerceIn(Settings.MIN_RADIUS_FLOOR, Settings.MAX_RADIUS_FLOOR),
            maxRadiusPx    = newSettings.maxRadiusPx.coerceIn(Settings.MIN_RADIUS_CEILING, Settings.MAX_RADIUS_CEILING)
                .coerceAtLeast(newSettings.minRadiusPx + 10f)   // max always > min
        )
        _settings.value = s

        // Rebuild bands if count changed
        if (s.bandCount != currentBands.count) {
            currentBands = BandDefinition.build(s.bandCount)
            bandSlots    = Array(s.bandCount) { arrayOfNulls(s.circlesPerBand) }
        } else if (s.circlesPerBand != bandSlots[0].size) {
            // Resize inner slot arrays
            bandSlots = Array(s.bandCount) { arrayOfNulls(s.circlesPerBand) }
        }
    }

    // ── Capture lifecycle ─────────────────────────────────────────────────────

    fun onPermissionGranted() {
        if (captureJob?.isActive == true) return
        _uiState.value = ChromaSoundUiState.Running(bandCount = _settings.value.bandCount)
        captureJob = viewModelScope.launch {
            engine.audioFrameFlow { currentBands }
                .catch { _uiState.value = ChromaSoundUiState.Idle }
                .collect { frame -> processFrame(frame) }
        }
    }

    fun onPermissionDenied() { _uiState.value = ChromaSoundUiState.PermissionDenied }

    fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        bandSlots.forEach { it.fill(null) }
        _uiState.value = ChromaSoundUiState.Idle
    }

    fun resumeCapture() { _uiState.value = ChromaSoundUiState.RequestingPermission }

    // ── Frame processing ──────────────────────────────────────────────────────

    private fun processFrame(frame: AudioFrame) {
        val nowMs = System.currentTimeMillis()
        val s     = _settings.value
        val bd    = currentBands

        // Resize slot grid if settings changed between frames
        if (bandSlots.size != bd.count || bandSlots[0].size != s.circlesPerBand) {
            bandSlots = Array(bd.count) { arrayOfNulls(s.circlesPerBand) }
        }

        // Expire dead circles in every slot
        for (band in 0 until bd.count) {
            for (slot in 0 until s.circlesPerBand) {
                val c = bandSlots[band][slot]
                if (c != null && !c.isAlive(nowMs)) bandSlots[band][slot] = null
            }
        }

        // Spawn new circles from peak bins
        val peakBins = frame.bandPeakBins
        for (band in 0 until minOf(bd.count, peakBins.size)) {
            val peakBin = peakBins[band]
            if (peakBin < 0) continue

            val db       = frame.decibelLevels.getOrElse(peakBin) { DB_FLOOR }
            val centreHz = bd.centreHz[band]

            // Find the oldest (lowest-life) slot to replace
            val targetSlot = (0 until s.circlesPerBand).minByOrNull { slot ->
                val c = bandSlots[band][slot]
                c?.lifeFraction(nowMs) ?: -1f   // null slots are "most expired"
            } ?: 0

            // Spread multiple circles within the band column with vertical jitter
            val bandFraction = (band + 0.5f) / bd.count
            val slotFraction  = if (s.circlesPerBand == 1) 0f
                                else (targetSlot / (s.circlesPerBand - 1f)) - 0.5f
            val y = (0.5f + slotFraction * 0.3f).coerceIn(0.1f, 0.9f)

            bandSlots[band][targetSlot] = FrequencyCircle(
                bandIndex    = band,
                slotIndex    = targetSlot,
                x            = bandFraction,
                y            = y,
                radiusPx     = dbToRadius(db, s.minRadiusPx, s.maxRadiusPx),
                color        = FrequencyColorMapper.frequencyToColor(centreHz),
                spawnTimeMs  = nowMs,
                lifetimeMs   = s.lifetimeMs,
                centreHz     = centreHz,
                decibelLevel = db
            )
        }

        val alive   = bandSlots.flatMap { it.filterNotNull() }
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
