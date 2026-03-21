package com.chromasound.app.fft

import kotlin.math.*

/**
 * Pure Kotlin Cooley-Tukey radix-2 FFT engine.
 *
 * Operates in-place on Complex arrays. Window size must be a power of 2.
 * Thread-safe: each call operates on its own arrays.
 */
object FFTEngine {

    // ── Window functions ────────────────────────────────────────────────────

    /**
     * Hann window — reduces spectral leakage between frequency bins.
     * Applied to the raw PCM samples before FFT.
     */
    fun hannWindow(size: Int): FloatArray {
        return FloatArray(size) { i ->
            (0.5f * (1f - cos(2.0 * PI * i / (size - 1)))).toFloat()
        }
    }

    // ── Cooley-Tukey iterative FFT ──────────────────────────────────────────

    /**
     * Compute the FFT of [samples] (already windowed PCM floats, length = power of 2).
     * Returns an array of [size/2] magnitude values (single-sided spectrum).
     *
     * Magnitude is normalized to [0, 1] relative to the maximum in this frame.
     */
    fun computeMagnitudes(samples: FloatArray): FloatArray {
        val n = samples.size
        require(n > 0 && (n and (n - 1)) == 0) { "FFT size must be a power of 2, got $n" }

        // Copy into complex arrays
        val real = DoubleArray(n) { samples[it].toDouble() }
        val imag = DoubleArray(n)

        fft(real, imag, n)

        // Single-sided magnitude spectrum (bins 0 .. n/2)
        val half = n / 2
        val magnitudes = FloatArray(half)
        var maxMag = 0f
        for (i in 0 until half) {
            val mag = sqrt(real[i] * real[i] + imag[i] * imag[i]).toFloat()
            magnitudes[i] = mag
            if (mag > maxMag) maxMag = mag
        }

        // Normalize
        if (maxMag > 0f) {
            for (i in 0 until half) magnitudes[i] /= maxMag
        }

        return magnitudes
    }

    /** In-place iterative Cooley-Tukey FFT (Decimation-In-Time). */
    private fun fft(real: DoubleArray, imag: DoubleArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                var tmp = real[i]; real[i] = real[j]; real[j] = tmp
                tmp = imag[i]; imag[i] = imag[j]; imag[j] = tmp
            }
        }

        // FFT butterfly stages
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angleStep = -2.0 * PI / len
            val wRealStep = cos(angleStep)
            val wImagStep = sin(angleStep)

            var i = 0
            while (i < n) {
                var wReal = 1.0
                var wImag = 0.0
                for (k in 0 until halfLen) {
                    val uReal = real[i + k]
                    val uImag = imag[i + k]
                    val vReal = real[i + k + halfLen] * wReal - imag[i + k + halfLen] * wImag
                    val vImag = real[i + k + halfLen] * wImag + imag[i + k + halfLen] * wReal

                    real[i + k] = uReal + vReal
                    imag[i + k] = uImag + vImag
                    real[i + k + halfLen] = uReal - vReal
                    imag[i + k + halfLen] = uImag - vImag

                    val newWReal = wReal * wRealStep - wImag * wImagStep
                    wImag = wReal * wImagStep + wImag * wRealStep
                    wReal = newWReal
                }
                i += len
            }
            len = len shl 1
        }
    }
}
