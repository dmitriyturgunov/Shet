package com.example.schet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class MenuActivity : AppCompatActivity() {

    private lateinit var addLevels: View
    private lateinit var subLevels: View
    private lateinit var tabAdd: Button
    private lateinit var tabSub: Button
    private lateinit var threshSlider: SeekBar
    private lateinit var threshValue: TextView
    private lateinit var winNameEl: TextView
    private lateinit var errNameEl: TextView

    private var winThreshold: Float = 0.5f

    // === Выбор своих звуков из галереи ===
    private val pickWinSound = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                prefs().edit().putString("customWinSound", it.toString()).apply()
                updateSoundLabels()
                Toast.makeText(this, "Звук победы установлен ✓", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось сохранить доступ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickErrSound = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                prefs().edit().putString("customErrSound", it.toString()).apply()
                updateSoundLabels()
                Toast.makeText(this, "Звук ошибки установлен ✓", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось сохранить доступ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        addLevels = findViewById(R.id.addLevels)
        subLevels = findViewById(R.id.subLevels)
        tabAdd = findViewById(R.id.tabAdd)
        tabSub = findViewById(R.id.tabSub)
        threshSlider = findViewById(R.id.threshSlider)
        threshValue = findViewById(R.id.threshValue)
        winNameEl = findViewById(R.id.winName)
        errNameEl = findViewById(R.id.errName)

        winThreshold = prefs().getFloat("winThreshold", 0.5f)
        threshSlider.max = 98
        threshSlider.progress = ((winThreshold - 0.2f) * 10).roundToInt()
        updateThresholdUI()

        tabAdd.setOnClickListener { switchTab(true) }
        tabSub.setOnClickListener { switchTab(false) }

        threshSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                winThreshold = 0.2f + progress * 0.1f
                updateThresholdUI()
                prefs().edit().putFloat("winThreshold", winThreshold).apply()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        findViewById<Button>(R.id.threshMinus).setOnClickListener {
            winThreshold = (winThreshold - 0.1f).coerceAtLeast(0.2f)
            threshSlider.progress = ((winThreshold - 0.2f) * 10).roundToInt()
        }
        findViewById<Button>(R.id.threshPlus).setOnClickListener {
            winThreshold = (winThreshold + 0.1f).coerceAtMost(10.0f)
            threshSlider.progress = ((winThreshold - 0.2f) * 10).roundToInt()
        }

        // Уровни
        bindLevel(R.id.add10, "add", 10)
        bindLevel(R.id.add20, "add", 20)
        bindLevel(R.id.add100, "add", 100)
        bindLevel(R.id.sub10, "sub", 10)
        bindLevel(R.id.sub20, "sub", 20)
        bindLevel(R.id.sub100, "sub", 100)

        // Свои звуки
        findViewById<Button>(R.id.pickWin).setOnClickListener {
            pickWinSound.launch(arrayOf("audio/*"))
        }
        findViewById<Button>(R.id.pickErr).setOnClickListener {
            pickErrSound.launch(arrayOf("audio/*"))
        }
        findViewById<Button>(R.id.resetWin).setOnClickListener {
            prefs().edit().remove("customWinSound").apply()
            updateSoundLabels()
        }
        findViewById<Button>(R.id.resetErr).setOnClickListener {
            prefs().edit().remove("customErrSound").apply()
            updateSoundLabels()
        }

        updateSoundLabels()
    }

    private fun prefs() = getSharedPreferences("schet", MODE_PRIVATE)

    private fun updateSoundLabels() {
        val winUri = prefs().getString("customWinSound", null)
        val errUri = prefs().getString("customErrSound", null)
        winNameEl.text = if (winUri != null) "📁 Свой звук установлен" else "Стандартные фанфары"
        errNameEl.text = if (errUri != null) "📁 Свой звук установлен" else "Стандартный (гул)"
    }

    private fun bindLevel(id: Int, op: String, max: Int) {
        findViewById<Button>(id).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java).apply {
                putExtra("op", op)
                putExtra("max", max)
            })
        }
    }

    private fun switchTab(isAdd: Boolean) {
        addLevels.visibility = if (isAdd) View.VISIBLE else View.GONE
        subLevels.visibility = if (isAdd) View.GONE else View.VISIBLE
        tabAdd.setBackgroundColor(if (isAdd) 0xFF3B82F6.toInt() else 0xFF1E293B.toInt())
        tabSub.setBackgroundColor(if (isAdd) 0xFF1E293B.toInt() else 0xFF3B82F6.toInt())
    }

    private fun updateThresholdUI() {
        threshValue.text = String.format("%.1f с", winThreshold)
    }
}
