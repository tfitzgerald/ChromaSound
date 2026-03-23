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
 * Captures microphone audio and emits [AudioFrame]s.
 *
 * The [bands] parameter is read each frame, so changing the [BandDefinition]
 * (e.g. from the settings slider) takes effect immediately without restarting
 * the audio capture loop.
 */
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
     * @param bands  A lambda that returns the *current* [BandDefinition].
     *               Called once per FFT frame so slider changes apply live.
     */
    fun audioFrameFlow(bands: () -> BandDefinition): Flow<AudioFrame> = flow {

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

                // Hann window + FFT
                for (i in 0 until FFT_SIZE) windowed[i] = pcm[i] * hannWindow[i]
                val magnitudes = FFTEngine.computeMagnitudes(windowed)

                // dBFS per bin
                val dBLevels = FloatArray(magnitudes.size) { i ->
                    val mag = magnitudes[i]
                    if (mag < 1e-10f) DB_FLOOR
                    else max(20f * log10(mag.toDouble()).toFloat(), DB_FLOOR)
                }

                // Fetch the current band definition (may have changed since last frame)
                val bd = bands()

                // For each band: loudest bin above threshold, or -1 if silent
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
