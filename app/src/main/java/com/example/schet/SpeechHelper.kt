package com.example.schet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class SpeechHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (Int) -> Unit
) {

    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    var isAvailable = false
        private set

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onEvent(eventType: Int, params: Bundle?) {}

                override fun onError(error: Int) {
                    isListening = false
                    onError(error)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        for (m in matches) onResult(m)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        for (m in matches) onResult(m)
                    }
                }
            })
            isAvailable = true
        }
    }

    fun start() {
        if (!isAvailable || isListening) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
        }
        try {
            recognizer?.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            isListening = false
        }
    }

    fun stop() {
        try { recognizer?.stopListening() } catch (e: Exception) {}
        try { recognizer?.cancel() } catch (e: Exception) {}
        isListening = false
    }

    fun destroy() {
        try { recognizer?.destroy() } catch (e: Exception) {}
        recognizer = null
    }

    companion object {
        val NUMBER_WORDS = mapOf(
            1 to listOf("один", "одна", "раз", "единица"),
            2 to listOf("два", "две", "двойка"),
            3 to listOf("три", "тройка"),
            4 to listOf("четыре", "четвёрка", "четверка"),
            5 to listOf("пять", "пятёрка", "пятерка"),
            6 to listOf("шесть", "шестёрка", "шестерка"),
            7 to listOf("семь", "семёрка", "семерка"),
            8 to listOf("восемь", "восьмёрка", "восьмерка"),
            9 to listOf("девять", "девятка"),
            10 to listOf("десять", "десятка"),
            11 to listOf("одиннадцать"),
            12 to listOf("двенадцать"),
            13 to listOf("тринадцать"),
            14 to listOf("четырнадцать"),
            15 to listOf("пятнадцать"),
            16 to listOf("шестнадцать"),
            17 to listOf("семнадцать"),
            18 to listOf("восемнадцать"),
            19 to listOf("девятнадцать"),
            20 to listOf("двадцать"),
            30 to listOf("тридцать"),
            40 to listOf("сорок"),
            50 to listOf("пятьдесят"),
            60 to listOf("шестьдесят"),
            70 to listOf("семьдесят"),
            80 to listOf("восемьдесят"),
            90 to listOf("девяносто"),
            100 to listOf("сто")
        )

        fun matches(text: String, correctSum: Int): Boolean {
            val t = text.lowercase(Locale("ru"))
                .replace("ё", "е")
                .replace(Regex("[^а-я0-9]"), " ")
                .trim()

            Regex("\\d+").findAll(t).forEach { m ->
                if (m.value.toIntOrNull() == correctSum) return true
            }

            val words = t.split(Regex("\\s+")).filter { it.isNotEmpty() }

            val expected = NUMBER_WORDS[correctSum] ?: emptyList()
            for (w in words) if (expected.contains(w)) return true

            if (correctSum > 20 && correctSum % 10 != 0) {
                val tens = (correctSum / 10) * 10
                val ones = correctSum % 10
                val tensWords = NUMBER_WORDS[tens] ?: emptyList()
                val onesWords = NUMBER_WORDS[ones] ?: emptyList()
                var tensSeen = false
                for (w in words) {
                    if (tensWords.contains(w)) tensSeen = true
                    else if (tensSeen && onesWords.contains(w)) return true
                }
            }
            return false
        }
    }
}
