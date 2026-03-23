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

/**
 * Captures raw microphone audio and emits [AudioFrame]s.
 *
 * @param bands       Lambda returning the current [BandDefinition] — read each frame.
 * @param sensitivity Lambda returning the current sensitivity multiplier (0.1 – 3.0).
 *                    Applied as a gain to raw dB levels before threshold comparison:
 *                    values > 1 make quiet sounds trigger circles, < 1 requires louder input.
 */
class AudioCaptureEngine {

    companion object {
        const val SAMPLE_RATE        = 44100
        const val FFT_SIZE           = 4096
        const val DB_FLOOR           = -80f
        const val DB_SPAWN_THRESHOLD = -50f   // base threshold before sensitivity scaling
        const val SILENCE_THRESHOLD  = 0.002f
    }

    private val hannWindow = FFTEngine.hannWindow(FFT_SIZE)

    fun audioFrameFlow(
        bands:       () -> BandDefinition,
        sensitivity: () -> Float
    ): Flow<AudioFrame> = flow {

        val minBuf     = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT
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

        try {
            recorder.startRecording()

            while (coroutineContext.isActive) {
                var totalRead = 0
                while (totalRead < FFT_SIZE && coroutineContext.isActive) {
                    val n = recorder.read(pcm, totalRead, FFT_SIZE - totalRead, AudioRecord.READ_BLOCKING)
                    if (n > 0) totalRead += n else break
                }
                if (totalRead < FFT_SIZE) continue

                // RMS amplitude
                var sumSq = 0.0
                for (s in pcm) sumSq += s * s
                val rms = sqrt(sumSq / FFT_SIZE).toFloat().coerceIn(0f, 1f)

                // Hann window + FFT
                for (i in 0 until FFT_SIZE) windowed[i] = pcm[i] * hannWindow[i]
                val magnitudes = FFTEngine.computeMagnitudes(windowed)

                // dBFS per bin — raw values before sensitivity
                val rawDb = FloatArray(magnitudes.size) { i ->
                    val mag = magnitudes[i]
                    if (mag < 1e-10f) DB_FLOOR
                    else max(20f * log10(mag.toDouble()).toFloat(), DB_FLOOR)
                }

                // Apply sensitivity: multiply dB values so they "shift" toward the threshold.
                // sensitivity = 1.0 → no change
                // sensitivity = 2.0 → a -60 dB signal is treated as -30 dB (easier to trigger)
                // sensitivity = 0.5 → a -30 dB signal is treated as -60 dB (harder to trigger)
                val gain = sensitivity().coerceIn(0.1f, 3.0f)
                val dBLevels = FloatArray(rawDb.size) { i ->
                    (rawDb[i] * gain).coerceIn(DB_FLOOR, 0f)
                }

                // Per-band: loudest boosted bin above threshold, or -1 if silent
                val bd = bands()
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

                emit(AudioFrame(magnitudes, rms, dBLevels, bandPeakBins))
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
