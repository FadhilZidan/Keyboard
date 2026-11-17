package com.example.soundboardkeyboard

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

data class Nota(val nama: String, val frekuensi: Double)

object NadaGenerator {

    val NOTA_PUTIH = listOf(
        Nota("C4", 261.63),
        Nota("D4", 293.66),
        Nota("E4", 329.63),
        Nota("F4", 349.23),
        Nota("G4", 392.00),
        Nota("A4", 440.00),
        Nota("B4", 493.88),
        Nota("C5", 523.25),
    )

    val NOTA_HITAM = listOf(
        Nota("C#4", 277.18),
        Nota("D#4", 311.13),
        Nota("F#4", 369.99),
        Nota("G#4", 415.30),
        Nota("A#4", 466.16),
    )

    val POSISI_HITAM = listOf(0, 1, 3, 4, 5)

    /**
     * Membuat data WAV 16-bit Mono 44100 Hz dengan gelombang sinus dan amplop peluruhan.
     */
    fun buatWav(frekuensi: Double, durasiDetik: Double = 1.0, sampleRate: Int = 44100): ByteArray {
        val numSamples = (sampleRate * durasiDetik).toInt()
        val dataSize = numSamples * 2
        val totalSize = 44 + dataSize

        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

        // Header RIFF/WAVE (44 Byte)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalSize - 8)
        buffer.put("WAVE".toByteArray())

        // Subchunk 1 "fmt "
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2)
        buffer.putShort(16)

        // Subchunk 2 "data"
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)

        // Payload Audio (PCM 16-bit)
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val wave = sin(2.0 * PI * frekuensi * t)

            val fadeIn = (i.toDouble() / 220.0).coerceAtMost(1.0)
            val decay = Math.exp(-3.5 * t)
            val fadeOut = ((numSamples - i).toDouble() / 1000.0).coerceIn(0.0, 1.0)

            val amp = wave * fadeIn * decay * fadeOut
            val sampleShort = (amp * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

            buffer.putShort(sampleShort)
        }

        return buffer.array()
    }

    /**
     * Menyimpan berkas WAV nada ke direktori cacheDir/nada/.
     */
    fun simpanWav(folderCache: File, nota: Nota): File {
        val folder = File(folderCache, "nada")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val namaSanitasi = nota.nama.replace("#", "s") + ".wav"
        val fileWav = File(folder, namaSanitasi)

        if (!fileWav.exists() || fileWav.length() == 0L) {
            val pcmData = buatWav(nota.frekuensi)
            FileOutputStream(fileWav).use { fos ->
                fos.write(pcmData)
            }
        }
        return fileWav
    }
}
