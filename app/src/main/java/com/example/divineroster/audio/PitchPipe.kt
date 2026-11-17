package com.example.divineroster.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * A reed-style tone that sounds while a note is held and fades out when it is released.
 * Frequencies are equal temperament with A4 = 440 Hz.
 */
class PitchPipe {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val A4_HZ = 440.0

        /** [semitone] counts from C of the given [octave] (C = 0 ... B = 11). */
        fun frequencyOf(semitone: Int, octave: Int): Double {
            val semitonesFromA4 = (octave - 4) * 12 + (semitone - 9)
            return A4_HZ * 2.0.pow(semitonesFromA4 / 12.0)
        }
    }

    private var current: Voice? = null

    /** Starts sounding [frequency]; any note still fading out keeps fading while the new one begins. */
    @Synchronized
    fun start(frequency: Double) {
        current?.release()
        current = Voice(frequency).also { it.start() }
    }

    @Synchronized
    fun stop() {
        current?.release()
        current = null
    }

    private class Voice(private val frequency: Double) {
        @Volatile
        private var releasing = false

        fun release() {
            releasing = true
        }

        fun start() {
            Thread({ play() }, "PitchPipeVoice").apply { isDaemon = true }.start()
        }

        private fun play() {
            val minBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            try {
                track.play()
                val buffer = ShortArray(512)
                val phaseStep = 2.0 * PI * frequency / SAMPLE_RATE
                // One-pole smoothing (~12 ms) gives a soft attack and release without clicks.
                val smoothing = 1.0 - exp(-1.0 / (SAMPLE_RATE * 0.012))
                var phase = 0.0
                var gain = 0.0

                while (true) {
                    val target = if (releasing) 0.0 else 1.0
                    for (i in buffer.indices) {
                        gain += (target - gain) * smoothing
                        // Fundamental plus two harmonics for a reed-like (harmonica) colour.
                        val wave = 0.65 * sin(phase) + 0.22 * sin(2 * phase) + 0.13 * sin(3 * phase)
                        buffer[i] = (wave * gain * 0.55 * Short.MAX_VALUE).toInt().toShort()
                        phase += phaseStep
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                    }
                    track.write(buffer, 0, buffer.size)
                    if (releasing && gain < 0.001) break
                }
            } finally {
                runCatching { track.stop() }
                track.release()
            }
        }
    }
}
