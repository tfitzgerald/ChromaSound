package com.chromasound.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.chromasound.app.fft.FFTEngine
import com.chromasound.app.model.AudioFrame
import com.chromasound.app.model.BandDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

class AudioCaptureEngine {

    companion object {
        const val SAMPLE_RATE        = 44100
        const val FFT_SIZE           = 4096
        const val DB_FLOOR           = -80f
        const val DB_SPAWN_THRESHOLD = -50f
        const val SILENCE_THRESHOLD  = 0.002f
    }

    private val hannWindow = FFTEngine.hannWindow(FFT_SIZE)

    /**
     * @param bands       Current [BandDefinition] — read each frame.
     * @param sensitivity dB gain multiplier — read each frame.
     * @param subBands    Number of sub-bands per band for shading — read each frame.
     */
    fun audioFrameFlow(
        bands:       () -> BandDefinition,
        sensitivity: () -> Float,
        subBands:    () -> Int
    ): Flow<AudioFrame> = flow {

        val minBuf     = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferSize = maxOf(minBuf, FFT_SIZE * 4)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT, bufferSize
        )
        require(recorder.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialise — check RECORD_AUDIO permission"
        }

        val pcm      = FloatArray(FFT_SIZE)
        val windowed = FloatArray(FFT_SIZE)

        try {
            recorder.startRecording()

            while (coroutineContext.isActive) {
                var totalRead = 0
                while (totalRead < FFT_SIZE && coroutineContext.isActive) {
                    val n = recorder.read(pcm, totalRead, FFT_SIZE - totalRead, AudioRecord.READ_BLOCKING)
                    if (n > 0) totalRead += n else break
                }
                if (totalRead < FFT_SIZE) continue

                // RMS
                var sumSq = 0.0
                for (s in pcm) sumSq += s * s
                val rms = sqrt(sumSq / FFT_SIZE).toFloat().coerceIn(0f, 1f)

                // Hann window + FFT
                for (i in 0 until FFT_SIZE) windowed[i] = pcm[i] * hannWindow[i]
                val magnitudes = FFTEngine.computeMagnitudes(windowed)

                // Raw dBFS per bin
                val rawDb = FloatArray(magnitudes.size) { i ->
                    val mag = magnitudes[i]
                    if (mag < 1e-10f) DB_FLOOR
                    else max(20f * log10(mag.toDouble()).toFloat(), DB_FLOOR)
                }

                // Apply sensitivity gain
                val gain     = sensitivity().coerceIn(0.1f, 3.0f)
                val dBLevels = FloatArray(rawDb.size) { i ->
                    (rawDb[i] * gain).coerceIn(DB_FLOOR, 0f)
                }

                val bd  = bands()
                val nsb = subBands().coerceIn(1, 12)

                // ── Per-band peak bin ─────────────────────────────────────────
                val bandPeakBins = IntArray(bd.count) { -1 }

                if (rms > SILENCE_THRESHOLD) {
                    val bandPeakDb = FloatArray(bd.count) { DB_SPAWN_THRESHOLD }
                    for (bin in 1 until magnitudes.size) {
                        val hz   = bin.toFloat() * SAMPLE_RATE / FFT_SIZE.toFloat()
                        val band = bd.bandFor(hz)
                        if (band < 0) continue
                        val db = dBLevels[bin]
                        if (db > bandPeakDb[band]) {
                            bandPeakDb[band]   = db
                            bandPeakBins[band] = bin
                        }
                    }
                }

                // ── Sub-band energies ─────────────────────────────────────────
                // For each band, split its frequency range into nsb equal (in log
                // space) slices. Average the magnitude of all FFT bins in each
                // slice, normalised to [0, 1] relative to the band's peak bin.
                val bandSubEnergies = Array(bd.count) { band ->
                    val loHz  = bd.lowerHz[band].toDouble()
                    val hiHz  = bd.upperHz[band].toDouble()
                    val logLo = Math.log10(loHz)
                    val logHi = Math.log10(hiHz)
                    val step  = (logHi - logLo) / nsb

                    // Peak magnitude in this band (for normalisation)
                    val peakBin = bandPeakBins[band]
                    val peakMag = if (peakBin >= 0 && peakBin < magnitudes.size)
                        magnitudes[peakBin].toDouble() else 0.0

                    FloatArray(nsb) { sub ->
                        val subLoHz = Math.pow(10.0, logLo + sub       * step)
                        val subHiHz = Math.pow(10.0, logLo + (sub + 1) * step)
                        val subLoBin = (subLoHz * FFT_SIZE / SAMPLE_RATE).toInt()
                            .coerceIn(1, magnitudes.size - 1)
                        val subHiBin = (subHiHz * FFT_SIZE / SAMPLE_RATE).toInt()
                            .coerceIn(subLoBin, magnitudes.size - 1)

                        // Average magnitude across all bins in this sub-band slice
                        var sum   = 0.0
                        var count = 0
                        for (bin in subLoBin..subHiBin) {
                            sum += magnitudes[bin]
                            count++
                        }
                        val avg = if (count > 0) sum / count else 0.0

                        // Normalise: 1.0 = as loud as the peak bin, 0.0 = silent
                        val energy = if (peakMag > 0.0) (avg / peakMag).toFloat() else 0f
                        energy.coerceIn(0f, 1f)
                    }
                }

                emit(AudioFrame(magnitudes, rms, dBLevels, bandPeakBins, bandSubEnergies))
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
