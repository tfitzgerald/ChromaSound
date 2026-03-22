package com.chromasound.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.chromasound.app.fft.FFTEngine
import com.chromasound.app.model.AudioFrame
import com.chromasound.app.model.FrequencyBands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Captures raw microphone audio and emits [AudioFrame]s via a Kotlin Flow.
 *
 * Per frame:
 *   PCM float (44100 Hz, 4096 samples) → Hann window → FFT
 *   → per-bin dBFS → per-band peak bin selection
 *
 * Frequency coverage: 30 Hz – 12 000 Hz split into 16 logarithmic bands.
 * Each band reports the single bin with the highest dB above the spawn
 * threshold, or -1 if the band is silent.
 */
class AudioCaptureEngine {

    companion object {
        const val SAMPLE_RATE        = 44100    // Hz
        const val FFT_SIZE           = 4096     // Must be power of 2
        const val DB_FLOOR           = -80f     // dBFS floor (treat as silence)
        const val DB_SPAWN_THRESHOLD = -50f     // dBFS — bands below this spawn nothing
        const val SILENCE_THRESHOLD  = 0.002f   // RMS below this → all bands silent
    }

    private val hannWindow = FFTEngine.hannWindow(FFT_SIZE)

    fun audioFrameFlow(): Flow<AudioFrame> = flow {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferSize = maxOf(minBuf, FFT_SIZE * 4)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            bufferSize
        )

        require(recorder.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialise — check RECORD_AUDIO permission"
        }

        val pcm      = FloatArray(FFT_SIZE)
        val windowed = FloatArray(FFT_SIZE)

        // Pre-compute which FFT bin belongs to which band.
        // bin → Hz:  hz = bin * SAMPLE_RATE / FFT_SIZE
        val binBand = IntArray(FFT_SIZE / 2) { bin ->
            val hz = bin.toFloat() * SAMPLE_RATE / FFT_SIZE.toFloat()
            FrequencyBands.bandFor(hz)   // -1 for out-of-range bins
        }

        try {
            recorder.startRecording()

            while (coroutineContext.isActive) {
                // Read exactly FFT_SIZE samples
                var totalRead = 0
                while (totalRead < FFT_SIZE && coroutineContext.isActive) {
                    val n = recorder.read(
                        pcm, totalRead, FFT_SIZE - totalRead, AudioRecord.READ_BLOCKING
                    )
                    if (n > 0) totalRead += n else break
                }
                if (totalRead < FFT_SIZE) continue

                // RMS
                var sumSq = 0.0
                for (s in pcm) sumSq += s * s
                val rms = sqrt(sumSq / FFT_SIZE).toFloat().coerceIn(0f, 1f)

                // Hann window
                for (i in 0 until FFT_SIZE) windowed[i] = pcm[i] * hannWindow[i]

                // FFT → normalised magnitudes
                val magnitudes = FFTEngine.computeMagnitudes(windowed)

                // dBFS per bin
                val decibelLevels = FloatArray(magnitudes.size) { i ->
                    val mag = magnitudes[i]
                    if (mag < 1e-10f) DB_FLOOR
                    else max(20f * log10(mag.toDouble()).toFloat(), DB_FLOOR)
                }

                // For each of the 16 bands: find the loudest bin above threshold
                // bandPeakBins[b] = bin index of loudest bin in band b, or -1 if silent
                val bandPeakBins = IntArray(FrequencyBands.COUNT) { -1 }

                if (rms > SILENCE_THRESHOLD) {
                    val bandPeakDb = FloatArray(FrequencyBands.COUNT) { DB_SPAWN_THRESHOLD }

                    for (bin in 1 until magnitudes.size) {   // skip DC bin 0
                        val band = binBand[bin]
                        if (band < 0) continue               // outside 30–12000 Hz

                        val db = decibelLevels[bin]
                        if (db > bandPeakDb[band]) {
                            bandPeakDb[band]   = db
                            bandPeakBins[band] = bin
                        }
                    }
                }

                emit(AudioFrame(magnitudes, rms, decibelLevels, bandPeakBins))
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
