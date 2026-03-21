package com.chromasound.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.chromasound.app.fft.FFTEngine
import com.chromasound.app.model.AudioFrame
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
 * Captures microphone audio and emits analysed [AudioFrame]s via a Kotlin Flow.
 *
 * Pipeline per frame:
 *   PCM float samples → Hann window → Cooley-Tukey FFT → magnitudes + dB levels
 *
 * Frame rate: SAMPLE_RATE / FFT_SIZE = 44100 / 4096 ≈ 10.77 frames/second
 * At that rate, 1.5 s = ~16 frames.
 */
class AudioCaptureEngine {

    companion object {
        const val SAMPLE_RATE       = 44100   // Hz
        const val FFT_SIZE          = 4096    // Power of 2; controls freq resolution & frame rate
        const val SILENCE_THRESHOLD = 0.003f  // RMS below this → emit no dominant bins

        // dB floor: bins quieter than this are ignored (avoids spawning circles for noise)
        const val DB_FLOOR = -60f             // dBFS
        // Only bins above this threshold spawn circles
        const val DB_SPAWN_THRESHOLD = -45f   // dBFS
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

        try {
            recorder.startRecording()

            while (coroutineContext.isActive) {
                // Collect exactly FFT_SIZE samples (blocking)
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

                // Hann window
                for (i in 0 until FFT_SIZE) windowed[i] = pcm[i] * hannWindow[i]

                // FFT → normalised magnitudes [0, 1]
                val magnitudes = FFTEngine.computeMagnitudes(windowed)

                // Convert each bin's magnitude to dBFS
                // dBFS = 20 * log10(magnitude); magnitude 0 → clamp to DB_FLOOR
                val decibelLevels = FloatArray(magnitudes.size) { i ->
                    val mag = magnitudes[i]
                    if (mag < 1e-10f) DB_FLOOR
                    else max(20f * log10(mag.toDouble()).toFloat(), DB_FLOOR)
                }

                // Dominant bins: all bins above DB_SPAWN_THRESHOLD (not just top N)
                // Skip bin 0 (DC component) and bin 1 (very low rumble)
                val dominantBins = if (rms > SILENCE_THRESHOLD) {
                    decibelLevels.mapIndexed { idx, db -> idx to db }
                        .drop(2)                               // skip DC + near-DC
                        .filter { (_, db) -> db > DB_SPAWN_THRESHOLD }
                        .sortedByDescending { (_, db) -> db }
                        .map { (idx, _) -> idx }
                } else {
                    emptyList()
                }

                emit(AudioFrame(magnitudes, rms, decibelLevels, dominantBins))
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
