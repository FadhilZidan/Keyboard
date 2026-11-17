package com.example.soundboardkeyboard

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var soundPool: SoundPool
    private val idSuara = mutableMapOf<String, Int>()
    private var jumlahTerisi = 0
    private val jumlahNada = 13
    private lateinit var tvStatus: TextView
    private lateinit var barisPutih: LinearLayout
    private lateinit var piano: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvStatus = findViewById(R.id.tvStatus)
        barisPutih = findViewById(R.id.barisPutih)
        piano = findViewById(R.id.piano)

        inisialisasiSoundPool()
        muatDanGenerasiAudio()
        renderTutsPutih()
        renderTutsHitam()
    }

    private fun inisialisasiSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                jumlahTerisi++
                if (jumlahTerisi == jumlahNada) {
                    runOnUiThread {
                        tvStatus.text = "Siap dimainkan"
                    }
                }
            }
        }
    }

    private fun muatDanGenerasiAudio() {
        tvStatus.text = "Memuat nada..."
        Thread {
            val semuaNota = NadaGenerator.NOTA_PUTIH + NadaGenerator.NOTA_HITAM
            semuaNota.forEach { nota ->
                val fileWav = NadaGenerator.simpanWav(cacheDir, nota)
                val soundId = soundPool.load(fileWav.absolutePath, 1)
                idSuara[nota.nama] = soundId
            }
        }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun renderTutsPutih() {
        barisPutih.removeAllViews()
        NadaGenerator.NOTA_PUTIH.forEach { nota ->
            val tutsPutih = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
                text = nota.nama
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, 0, dpToPx(16))
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundResource(R.drawable.bg_white_key_normal)
                setTextColor(0xFF1E293B.toInt())

                pasangListenerTuts(this, nota, isWhiteKey = true)
            }
            barisPutih.addView(tutsPutih)
        }
    }

    private fun renderTutsHitam() {
        piano.post {
            val jumlahPutih = NadaGenerator.NOTA_PUTIH.size
            if (barisPutih.width <= 0) return@post

            val lebarPutih = barisPutih.width.toFloat() / jumlahPutih
            val lebarHitam = lebarPutih * 0.6f
            val tinggiHitam = barisPutih.height.toFloat() * 0.6f

            NadaGenerator.NOTA_HITAM.forEachIndexed { i, nota ->
                val posIndex = NadaGenerator.POSISI_HITAM[i]
                val posX = (posIndex + 1) * lebarPutih - (lebarHitam / 2f)

                val tutsHitam = TextView(this).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        lebarHitam.toInt(),
                        tinggiHitam.toInt()
                    )
                    x = posX
                    text = nota.nama
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    setPadding(0, 0, 0, dpToPx(12))
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setBackgroundResource(R.drawable.bg_black_key_normal)
                    setTextColor(Color.WHITE)

                    pasangListenerTuts(this, nota, isWhiteKey = false)
                }
                piano.addView(tutsHitam)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun pasangListenerTuts(
        view: TextView,
        nota: Nota,
        isWhiteKey: Boolean
    ) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    setKeyActive(view, isWhiteKey)
                    mainkan(nota.nama)
                    v.isPressed = true
                    true
                }
                MotionEvent.ACTION_UP -> {
                    setKeyNormal(view, isWhiteKey)
                    v.isPressed = false
                    v.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    setKeyNormal(view, isWhiteKey)
                    v.isPressed = false
                    true
                }
                else -> false
            }
        }
    }

    private fun setKeyActive(view: TextView, isWhiteKey: Boolean) {
        view.setBackgroundResource(
            if (isWhiteKey) R.drawable.bg_white_key_pressed else R.drawable.bg_black_key_pressed
        )
        view.setTextColor(Color.WHITE)
    }

    private fun setKeyNormal(view: TextView, isWhiteKey: Boolean) {
        if (isWhiteKey) {
            view.setBackgroundResource(R.drawable.bg_white_key_normal)
            view.setTextColor(0xFF1E293B.toInt())
        } else {
            view.setBackgroundResource(R.drawable.bg_black_key_normal)
            view.setTextColor(Color.WHITE)
        }
    }

    private fun mainkan(namaNada: String) {
        val soundId = idSuara[namaNada]
        if (soundId != null && soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
            tvStatus.text = "Memainkan $namaNada"
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }
}
