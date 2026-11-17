package com.example.divineroster.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sample-accurate metronome: every beat is a short click followed by silence, written straight to
 * an AudioTrack, so the tempo can't drift the way a timer-based loop would.
 */
class Metronome(private val onBeat: (beatIndex: Int) -> Unit) {

    companion object {
        const val MIN_BPM = 30
        const val MAX_BPM = 240
        private const val SAMPLE_RATE = 44100
        private const val CLICK_MS = 28
        private const val CHUNK = 1024
    }

    @Volatile
    var bpm: Int = 100
        set(value) {
            field = value.coerceIn(MIN_BPM, MAX_BPM)
        }

    @Volatile
    var beatsPerBar: Int = 4
        set(value) {
            field = value.coerceAtLeast(1)
        }

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    val isRunning: Boolean get() = running

    @Synchronized
    fun start() {
        if (running) return
        running = true
        thread = Thread({ run() }, "MetronomeThread").apply { isDaemon = true; start() }
    }

    @Synchronized
    fun stop() {
        running = false
        thread = null
    }

    private fun clickSound(accent: Boolean): ShortArray {
        val length = SAMPLE_RATE * CLICK_MS / 1000
        val frequency = if (accent) 1600.0 else 1000.0
        return ShortArray(length) { i ->
            val envelope = exp(-6.0 * i / length)
            (sin(2.0 * PI * frequency * i / SAMPLE_RATE) * envelope * 0.9 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun run() {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

        val accentClick = clickSound(accent = true)
        val normalClick = clickSound(accent = false)
        val silence = ShortArray(CHUNK)

        try {
            track.play()
            var beat = 0
            while (running) {
                val beatIndex = beat % beatsPerBar
                mainHandler.post { onBeat(beatIndex) }

                val samplesPerBeat = SAMPLE_RATE * 60 / bpm
                val click = if (beatIndex == 0) accentClick else normalClick
                track.write(click, 0, minOf(click.size, samplesPerBeat))

                // Fill the rest of the beat with silence in small chunks so Stop reacts quickly.
                var remaining = samplesPerBeat - click.size
                while (remaining > 0 && running) {
                    val count = minOf(CHUNK, remaining)
                    track.write(silence, 0, count)
                    remaining -= count
                }
                beat++
            }
        } finally {
            runCatching { track.pause(); track.flush(); track.stop() }
            track.release()
        }
    }
}
