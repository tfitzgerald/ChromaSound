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
import kotlin.random.Random

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

    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    @Volatile private var currentBands = BandDefinition.build(BandDefinition.DEFAULT_BANDS)

    @Volatile private var bandSlots =
        Array(BandDefinition.DEFAULT_BANDS) { arrayOfNulls<FrequencyCircle>(1) }

    private var captureJob: Job? = null

    companion object {
        fun formatHz(hz: Float) =
            if (hz >= 1000f) "${"%.1f".format(hz / 1000f)} kHz" else "${hz.toInt()} Hz"

        fun dbToRadius(db: Float, minPx: Float, maxPx: Float): Float {
            val range      = 0f - DB_SPAWN_THRESHOLD
            val normalized = (db - DB_SPAWN_THRESHOLD) / range
            return (minPx + normalized.coerceIn(0f, 1f) * (maxPx - minPx))
        }

        fun computeX(band: Int, bandCount: Int, placement: Float): Float {
            val centre    = (band + 0.5f) / bandCount
            val halfBand  = 0.5f / bandCount
            val maxOffset = halfBand + placement * (0.5f - halfBand)
            val jitter    = (Random.nextFloat() * 2f - 1f) * maxOffset * placement
            return (centre + jitter).coerceIn(0.02f, 0.98f)
        }

        fun computeY(placement: Float): Float {
            val maxOffset = placement * 0.45f
            val jitter    = (Random.nextFloat() * 2f - 1f) * maxOffset
            return (0.5f + jitter).coerceIn(0.05f, 0.95f)
        }
    }

    // ── Settings API ──────────────────────────────────────────────────────────

    fun updateSettings(new: Settings) {
        val s = new.copy(
            bandCount      = new.bandCount.coerceIn(BandDefinition.MIN_BANDS, BandDefinition.MAX_BANDS),
            lifetimeMs     = new.lifetimeMs.coerceIn(Settings.MIN_LIFETIME_MS, Settings.MAX_LIFETIME_MS),
            circlesPerBand = new.circlesPerBand.coerceIn(Settings.MIN_CIRCLES_PER_BAND, Settings.MAX_CIRCLES_PER_BAND),
            minRadiusPx    = new.minRadiusPx.coerceIn(Settings.MIN_RADIUS_FLOOR, Settings.MAX_RADIUS_FLOOR),
            maxRadiusPx    = new.maxRadiusPx.coerceIn(Settings.MIN_RADIUS_CEILING, Settings.MAX_RADIUS_CEILING)
                .coerceAtLeast(new.minRadiusPx + 10f),
            placement      = new.placement.coerceIn(Settings.MIN_PLACEMENT, Settings.MAX_PLACEMENT),
            sensitivity    = new.sensitivity.coerceIn(Settings.MIN_SENSITIVITY, Settings.MAX_SENSITIVITY),
            subBands       = new.subBands.coerceIn(Settings.MIN_SUB_BANDS, Settings.MAX_SUB_BANDS)
        )
        _settings.value = s

        if (s.bandCount != currentBands.count) {
            currentBands = BandDefinition.build(s.bandCount)
            bandSlots    = Array(s.bandCount) { arrayOfNulls(s.circlesPerBand) }
        } else if (s.circlesPerBand != bandSlots[0].size) {
            bandSlots = Array(s.bandCount) { arrayOfNulls(s.circlesPerBand) }
        }
    }

    // ── Capture lifecycle ─────────────────────────────────────────────────────

    fun onPermissionGranted() {
        if (captureJob?.isActive == true) return
        _uiState.value = ChromaSoundUiState.Running(bandCount = _settings.value.bandCount)
        captureJob = viewModelScope.launch {
            engine.audioFrameFlow(
                bands       = { currentBands },
                sensitivity = { _settings.value.sensitivity },
                subBands    = { _settings.value.subBands }
            )
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

        if (bandSlots.size != bd.count || bandSlots[0].size != s.circlesPerBand) {
            bandSlots = Array(bd.count) { arrayOfNulls(s.circlesPerBand) }
        }

        // Expire dead circles
        for (band in 0 until bd.count)
            for (slot in 0 until s.circlesPerBand) {
                val c = bandSlots[band][slot]
                if (c != null && !c.isAlive(nowMs)) bandSlots[band][slot] = null
            }

        // Spawn new circles
        val peakBins = frame.bandPeakBins
        for (band in 0 until minOf(bd.count, peakBins.size)) {
            val peakBin = peakBins[band]
            if (peakBin < 0) continue

            val db       = frame.decibelLevels.getOrElse(peakBin) { DB_FLOOR }
            val centreHz = bd.centreHz[band]

            val targetSlot = (0 until s.circlesPerBand).minByOrNull { slot ->
                bandSlots[band][slot]?.lifeFraction(nowMs) ?: -1f
            } ?: 0

            val y = computeY(s.placement)
            val x = computeX(band, bd.count, s.placement)

            // Retrieve sub-band energies for this band from the audio frame
            val subEnergies = if (band < frame.bandSubEnergies.size)
                frame.bandSubEnergies[band]
            else
                FloatArray(s.subBands) { 1f }

            bandSlots[band][targetSlot] = FrequencyCircle(
                bandIndex       = band,
                slotIndex       = targetSlot,
                x               = x,
                y               = y,
                radiusPx        = dbToRadius(db, s.minRadiusPx, s.maxRadiusPx),
                color           = FrequencyColorMapper.frequencyToColor(centreHz, s.colorScheme),
                spawnTimeMs     = nowMs,
                lifetimeMs      = s.lifetimeMs,
                centreHz        = centreHz,
                decibelLevel    = db,
                subBandEnergies = subEnergies
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
