package com.example.schet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class GameActivity : AppCompatActivity() {

    private lateinit var letterText: TextView
    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var btnStart: Button
    private lateinit var btnBack: Button
    private lateinit var levelLabel: TextView
    private lateinit var micIndicator: TextView

    private lateinit var soundPlayer: SoundPlayer
    private var speechHelper: SpeechHelper? = null
    private var customPlayer: MediaPlayer? = null

    private var currentOp = "add"
    private var currentLevel = 10
    private var problems: List<Problem> = emptyList()
    private val weights = mutableMapOf<String, Double>()
    private val times = mutableMapOf<String, MutableList<Long>>()

    private var currentProblem: Problem? = null
    private var shownAt = 0L
    private var playing = false
    private var totalSolved = 0
    private val allTimes = mutableListOf<Long>()
    private var bestTime: Long? = null
    private var roundActive = false
    private var answeredThisRound = false
    private var inputValue = ""

    private var winThreshold = 0.5f

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        letterText = findViewById(R.id.letterText)
        statusText = findViewById(R.id.statusText)
        statsText = findViewById(R.id.statsText)
        btnStart = findViewById(R.id.btnStart)
        btnBack = findViewById(R.id.btnBack)
        levelLabel = findViewById(R.id.levelLabel)
        micIndicator = findViewById(R.id.micIndicator)

        soundPlayer = SoundPlayer()

        currentOp = intent.getStringExtra("op") ?: "add"
        currentLevel = intent.getIntExtra("max", 10)

        problems = ProblemGenerator.build(currentOp, currentLevel)
        problems.forEach {
            weights[it.key] = 1.0
            times[it.key] = mutableListOf()
        }

        levelLabel.text = if (currentOp == "add")
            "Сложение до $currentLevel"
        else
            "Вычитание до $currentLevel"

        winThreshold = getSharedPreferences("schet", MODE_PRIVATE)
            .getFloat("winThreshold", 0.5f)

        setupNumberButtons()
        findViewById<Button>(R.id.delBtn).setOnClickListener { onDelete() }
        findViewById<Button>(R.id.checkBtn).setOnClickListener { onCheck() }
        findViewById<Button>(R.id.skipBtn).setOnClickListener { submitAnswer(-1) }
        btnBack.setOnClickListener { finish() }
        btnStart.setOnClickListener { if (playing) stopGame() else startGame() }

        // Запрос разрешения микрофона
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            initSpeech()
        }
    }

    private fun setupNumberButtons() {
        val ids = mapOf(
            R.id.num0 to 0, R.id.num1 to 1, R.id.num2 to 2, R.id.num3 to 3,
            R.id.num4 to 4, R.id.num5 to 5, R.id.num6 to 6, R.id.num7 to 7,
            R.id.num8 to 8, R.id.num9 to 9
        )
        for ((id, num) in ids) {
            findViewById<Button>(id).setOnClickListener {
                if (!playing || !roundActive || answeredThisRound) return@setOnClickListener
                if (inputValue.length >= 3) return@setOnClickListener
                inputValue += num.toString()
                statusText.text = "Введено: $inputValue"
                statusText.setTextColor(0xFF60A5FA.toInt())
            }
        }
    }

    private fun onDelete() {
        if (!playing || !roundActive || answeredThisRound) return
        inputValue = inputValue.dropLast(1)
        statusText.text = if (inputValue.isEmpty()) "Сколько получится?" else "Введено: $inputValue"
        statusText.setTextColor(if (inputValue.isEmpty()) 0xFF9FB3C8.toInt() else 0xFF60A5FA.toInt())
    }

    private fun onCheck() {
        if (!playing || !roundActive || answeredThisRound) return
        if (inputValue.isEmpty()) {
            statusText.text = "Введите ответ цифрами"
            statusText.setTextColor(0xFFEF4444.toInt())
            return
        }
        submitAnswer(inputValue.toIntOrNull() ?: -1)
    }

    private fun initSpeech() {
        speechHelper = SpeechHelper(
            this,
            onResult = { text ->
                if (!playing || answeredThisRound) return@SpeechHelper
                val cur = currentProblem ?: return@SpeechHelper
                runOnUiThread {
                    statusText.text = "🎤 «$text» · нужно: ${cur.sum}"
                    statusText.setTextColor(0xFF60A5FA.toInt())
                }
                if (SpeechHelper.matches(text, cur.sum)) {
                    answeredThisRound = true
                    runOnUiThread { submitAnswer(cur.sum) }
                }
            },
            onError = { error ->
                if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    // тишина — просто слушаем дальше
                }
                if (playing && !answeredThisRound) {
                    handler.postDelayed({ safeStartSpeech() }, 400)
                }
            }
        )
        micIndicator.text = if (speechHelper?.isAvailable == true) "🎤 Готов" else "🎤 Недоступно"
    }

    private fun safeStartSpeech() {
        if (playing && !answeredThisRound) {
            speechHelper?.start()
            micIndicator.text = "🎤 Слушаю…"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initSpeech()
        }
    }

    private fun startGame() {
        playing = true
        totalSolved = 0
        allTimes.clear()
        bestTime = null
        inputValue = ""
        problems.forEach {
            weights[it.key] = 1.0
            times[it.key]?.clear()
        }
        btnStart.text = "СТОП"
        btnStart.setBackgroundColor(0xFFEF4444.toInt())
        updateStats()
        handler.postDelayed({ if (playing) nextProblem() }, 300)
    }

    private fun stopGame() {
        playing = false
        roundActive = false
        answeredThisRound = true
        speechHelper?.stop()
        btnStart.text = "СТАРТ"
        btnStart.setBackgroundColor(0xFF3B82F6.toInt())
        letterText.text = "?"
        letterText.setTextColor(0xFFFFFFFF.toInt())
        statusText.text = "Нажмите СТАРТ"
        statusText.setTextColor(0xFF9FB3C8.toInt())
    }

    private fun pickWeighted(): Problem {
        val total = problems.sumOf { weights[it.key] ?: 1.0 }
        var r = Math.random() * total
        for (p in problems) {
            r -= weights[p.key] ?: 1.0
            if (r <= 0) return p
        }
        return problems.random()
    }

    private fun nextProblem() {
        if (!playing) return
        answeredThisRound = false
        roundActive = true
        inputValue = ""

        currentProblem = pickWeighted()
        val p = currentProblem!!
        val sign = if (currentOp == "add") "+" else "−"
        letterText.text = "${p.a} $sign ${p.b} = ?"
        letterText.setTextColor(0xFFFFFFFF.toInt())
        statusText.text = "Сколько получится?"
        statusText.setTextColor(0xFF9FB3C8.toInt())

        if (speechHelper?.isAvailable == true) {
            safeStartSpeech()
        }

        shownAt = System.currentTimeMillis()
    }

    private fun submitAnswer(userAnswer: Int) {
        if (!playing || !roundActive) return
        roundActive = false
        answeredThisRound = true

        val elapsed = System.currentTimeMillis() - shownAt
        val sec = elapsed / 1000.0

        speechHelper?.stop()

        val p = currentProblem!!
        val correct = p.sum
        val sign = if (currentOp == "add") "+" else "−"

        if (userAnswer == correct) {
            letterText.setTextColor(0xFF66E0A3.toInt())
            handler.postDelayed({
                letterText.text = "${p.a} $sign ${p.b} = $correct"

                if (sec < winThreshold) {
                    letterText.setTextColor(0xFFFFD166.toInt())
                    statusText.text = "🎉 Победа! ${"%.2f".format(sec)} сек"
                    statusText.setTextColor(0xFFFFD166.toInt())
                    playCustomOrBuiltin("customWinSound") { soundPlayer.playWin() }
                } else {
                    statusText.text = "✓ Верно: $correct · ${"%.2f".format(sec)} сек"
                    statusText.setTextColor(0xFF66E0A3.toInt())
                }
            }, 100)

            val key = p.key
            times[key]?.add(elapsed)
            if ((times[key]?.size ?: 0) > 5) times[key]?.removeAt(0)
            totalSolved++
            allTimes.add(elapsed)
            if (allTimes.size > 50) allTimes.removeAt(0)
            if (bestTime == null || elapsed < bestTime!!) bestTime = elapsed

            val avg = times[key]?.average() ?: 1000.0
            weights[key] = 1.0 + (avg / 500.0).coerceAtMost(5.0)

            updateStats()

            val delay = if (sec < winThreshold) 2200L else 1200L
            handler.postDelayed({ if (playing) nextProblem() }, delay)

        } else {
            playCustomOrBuiltin("customErrSound") { soundPlayer.playError() }

            letterText.setTextColor(0xFFEF4444.toInt())
            statusText.text = "✗ Неправильно. Попробуй ещё!"
            statusText.setTextColor(0xFFEF4444.toInt())

            handler.postDelayed({
                if (!playing) return@postDelayed
                letterText.text = "${p.a} $sign ${p.b} = $correct"
                letterText.setTextColor(0xFF66E0A3.toInt())
                statusText.text = "Правильный ответ: $correct"
                statusText.setTextColor(0xFF66E0A3.toInt())

                val key = p.key
                times[key]?.add(3000L)
                if ((times[key]?.size ?: 0) > 5) times[key]?.removeAt(0)
                val avg = times[key]?.average() ?: 1000.0
                weights[key] = 1.0 + (avg / 500.0).coerceAtMost(5.0)
            }, 800)

            handler.postDelayed({ if (playing) nextProblem() }, 2500)
        }
    }

    // Проигрывание кастомного звука, если задан; иначе — встроенный
    private fun playCustomOrBuiltin(prefKey: String, fallback: () -> Unit) {
        val uriStr = getSharedPreferences("schet", MODE_PRIVATE)
            .getString(prefKey, null)
        if (uriStr == null) {
            fallback()
            return
        }
        try {
            customPlayer?.release()
            customPlayer = MediaPlayer().apply {
                setDataSource(this@GameActivity, Uri.parse(uriStr))
                setOnPreparedListener { start() }
                setOnCompletionListener { it.release(); customPlayer = null }
                setOnErrorListener { _, _, _ ->
                    fallback()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            fallback()
        }
    }

    private fun updateStats() {
        val avgAll = if (allTimes.isNotEmpty())
            "%.2f".format(allTimes.average() / 1000.0)
        else "—"
        val best = bestTime?.let { "%.2f".format(it / 1000.0) } ?: "—"
        statsText.text = "Решено: $totalSolved · Среднее: $avgAll с · Лучшее: $best с"
    }

    override fun onDestroy() {
        super.onDestroy()
        speechHelper?.destroy()
        customPlayer?.release()
        handler.removeCallbacksAndMessages(null)
    }
}
