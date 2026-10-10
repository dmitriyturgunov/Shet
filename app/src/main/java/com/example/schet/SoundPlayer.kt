package com.example.schet

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundPlayer {

    private val sampleRate = 44100

    // 🎺 Фанфары победы
    fun playWin() {
        Thread {
            try {
                val duration = 1.5
                val totalSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(totalSamples)

                val C5 = 523.25
                val E5 = 659.25
                val G5 = 783.99
                val C6 = 1046.5
                val C3 = 130.81
                val G3 = 196.00
                val C4 = 261.63

                mixNotes(buffer, listOf(C5, E5, G5), 0.0, 0.18, 0.18)
                mixNote(buffer, C3, 0.0, 0.20, 0.22)

                mixNotes(buffer, listOf(E5, G5, C6), 0.25, 0.18, 0.18)
                mixNote(buffer, G3, 0.25, 0.20, 0.22)

                mixNotes(buffer, listOf(C5, E5, G5, C6), 0.50, 0.80, 0.18)
                mixNote(buffer, C3, 0.50, 0.80, 0.22)
                mixNote(buffer, C4, 0.50, 0.80, 0.15)

                mixNote(buffer, C6, 1.10, 0.35, 0.15)
                mixNote(buffer, C3, 1.10, 0.40, 0.20)

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // 😱 Страшный звук при ошибке
    fun playError() {
        Thread {
            try {
                val duration = 0.45
                val totalSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(totalSamples)

                val o1Start = 220.0
                val o1End = 90.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = t / duration
                    val phase = 2 * PI * (o1Start * t + (o1End - o1Start) * t * t / (2 * duration))
                    val saw = ((phase / (2 * PI)) % 1.0) * 2 - 1
                    val env = exp(-3.0 * progress)
                    val sample = (saw * env * 0.35 * 32767).toInt()
                    buffer[i] = (buffer[i] + sample).coerceIn(-32768, 32767).toShort()
                }

                val o2Start = 146.83
                val o2End = 60.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = t / duration
                    val phase = 2 * PI * (o2Start * t + (o2End - o2Start) * t * t / (2 * duration))
                    val square = if (sin(phase) >= 0) 1.0 else -1.0
                    val env = exp(-3.0 * progress)
                    val sample = (square * env * 0.18 * 32767).toInt()
                    buffer[i] = (buffer[i] + sample).coerceIn(-32768, 32767).toShort()
                }

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    if (t < 0.1) {
                        val noise = (Random.nextDouble() * 2 - 1) * exp(-30 * t) * 0.08 * 32767
                        buffer[i] = (buffer[i] + noise.toInt()).coerceIn(-32768, 32767).toShort()
                    }
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun mixNote(
        buffer: ShortArray,
        freq: Double,
        startSec: Double,
        durationSec: Double,
        volume: Double
    ) {
        val startSample = (startSec * sampleRate).toInt()
        val endSample = ((startSec + durationSec) * sampleRate).toInt()
        val attack = 0.03
        val release = durationSec * 0.3

        for (i in startSample until endSample.coerceAtMost(buffer.size)) {
            val t = (i - startSample).toDouble() / sampleRate

            val env = when {
                t < attack -> t / attack
                t > durationSec - release -> ((durationSec - t) / release).coerceAtLeast(0.0)
                else -> 1.0
            }.coerceIn(0.0, 1.0)

            val vibrato = sin(2 * PI * 5.5 * t) * 4.0
            val actualFreq = freq + vibrato

            val phase = 2 * PI * actualFreq * t
            val saw = ((phase / (2 * PI)) % 1.0) * 2 - 1

            val sample = saw * env * volume
            val existing = buffer[i].toInt()
            val newVal = existing + (sample * 32767).toInt()
            buffer[i] = newVal.coerceIn(-32768, 32767).toShort()
        }
    }

    private fun mixNotes(
        buffer: ShortArray,
        freqs: List<Double>,
        startSec: Double,
        durationSec: Double,
        volume: Double
    ) {
        for (f in freqs) {
            mixNote(buffer, f, startSec, durationSec, volume / freqs.size * 1.5)
        }
    }

    private fun playBuffer(buffer: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            val delay = (buffer.size * 1000L / sampleRate) + 100
            Thread {
                try {
                    Thread.sleep(delay)
                    track.stop()
                    track.release()
                } catch (e: Exception) { }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
