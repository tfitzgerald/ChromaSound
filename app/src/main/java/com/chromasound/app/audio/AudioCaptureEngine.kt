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
import kotlin.math.sqrt

/**
 * Audio capture and FFT analysis pipeline.
 *
 * Uses [AudioRecord] with UNPROCESSED source (bypasses AGC and noise suppression)
 * to capture raw PCM at 44100 Hz. Applies a Hann window then feeds each frame
 * through [FFTEngine] before emitting [AudioFrame] snapshots.
 *
 * The returned [Flow] runs on [Dispatchers.IO] and should be collected in a
 * coroutine that is cancelled when capture should stop (e.g. ViewModel lifecycle).
 */
class AudioCaptureEngine {

    companion object {
        const val SAMPLE_RATE = 44100          // Hz
        const val FFT_SIZE = 4096              // Must be power of 2
        const val DOMINANT_BIN_COUNT = 12      // How many top bins to track
        const val SILENCE_THRESHOLD = 0.005f   // RMS below this = silence
    }

    private val hannWindow = FFTEngine.hannWindow(FFT_SIZE)

    /**
     * Continuously captures audio and emits analyzed [AudioFrame]s.
     * The flow terminates when the collecting coroutine is cancelled.
     */
    fun audioFrameFlow(): Flow<AudioFrame> = flow {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        // Use at least FFT_SIZE floats (4 bytes each) so we always have a full window
        val bufferSize = maxOf(minBufferSize, FFT_SIZE * 4)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.UNPROCESSED,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            bufferSize
        )

        require(recorder.state == AudioRecord.STATE_INITIALIZED) {
            "AudioRecord failed to initialize — check RECORD_AUDIO permission"
        }

        val pcmBuffer = FloatArray(FFT_SIZE)
        val windowedBuffer = FloatArray(FFT_SIZE)

        try {
            recorder.startRecording()

            while (coroutineContext.isActive) {
                // Read exactly FFT_SIZE samples (blocking until available)
                var totalRead = 0
                while (totalRead < FFT_SIZE && coroutineContext.isActive) {
                    val read = recorder.read(
                        pcmBuffer,
                        totalRead,
                        FFT_SIZE - totalRead,
                        AudioRecord.READ_BLOCKING
                    )
                    if (read > 0) totalRead += read else break
                }
                if (totalRead < FFT_SIZE) continue

                // Compute RMS volume from raw PCM
                var sumSq = 0.0
                for (i in 0 until FFT_SIZE) sumSq += pcmBuffer[i] * pcmBuffer[i]
                val rms = sqrt(sumSq / FFT_SIZE).toFloat().coerceIn(0f, 1f)

                // Apply Hann window to reduce spectral leakage
                for (i in 0 until FFT_SIZE) windowedBuffer[i] = pcmBuffer[i] * hannWindow[i]

                // Compute FFT magnitudes
                val magnitudes = FFTEngine.computeMagnitudes(windowedBuffer)

                // Find dominant frequency bins (top DOMINANT_BIN_COUNT by magnitude)
                // Skip bin 0 (DC offset) and use logarithmic bin spacing
                val dominantBins = if (rms > SILENCE_THRESHOLD) {
                    magnitudes.mapIndexed { idx, mag -> idx to mag }
                        .drop(1) // skip DC
                        .sortedByDescending { it.second }
                        .take(DOMINANT_BIN_COUNT)
                        .map { it.first }
                } else {
                    emptyList()
                }

                emit(AudioFrame(magnitudes, rms, dominantBins))
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
