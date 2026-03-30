package com.jarvis.mobile.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class JarvisTTS private constructor(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.05f) // matches desktop rate
                tts?.setPitch(0.95f)    // matches desktop pitch
                isReady = true
            }
        }
    }

    fun speak(text: String) {
        if (!isReady) return
        val cleaned = toSpeech(text)
        if (cleaned.isBlank()) return
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "jarvis_${System.currentTimeMillis()}")
    }

    /** Port of the desktop toSpeech() function — strips markdown before speaking. */
    private fun toSpeech(text: String): String = text
        // Strip <think> blocks entirely
        .replace(Regex("<think>[\\s\\S]*?</think>"), "")
        // Replace full code blocks with a spoken note
        .replace(Regex("```[\\s\\S]*?```"), "... I've written the code for you, Sir, check the chat.")
        // Strip inline code backticks
        .replace(Regex("`([^`]+)`"), "$1")
        // Strip markdown bold / italic
        .replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")
        // Strip markdown headers — keep text
        .replace(Regex("(?m)^#{1,6}\\s+"), "")
        // Strip URLs entirely
        .replace(Regex("https?://[^\\s)>]+"), "")
        // Convert markdown links — read just the label
        .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
        // Convert bullet / numbered list markers to natural pauses
        .replace(Regex("(?m)^\\s*[-*•]\\s+"), "... ")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "... ")
        // Strip HTML tags
        .replace(Regex("<[^>]+>"), "")
        // Strip leftover special characters
        .replace(Regex("[#|>_~^]"), "")
        .replace("────────────────────", "")
        // Collapse multiple ellipses
        .replace(Regex("(\\.\\.\\.\\s*)+"), "... ")
        // Collapse excessive whitespace / blank lines
        .replace(Regex("\\n{2,}"), "... ")
        .replace("\n", " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

    fun stop() {
        tts?.stop()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        instance = null
    }

    companion object {
        @Volatile
        private var instance: JarvisTTS? = null

        fun getInstance(context: Context): JarvisTTS {
            return instance ?: synchronized(this) {
                instance ?: JarvisTTS(context.applicationContext).also { instance = it }
            }
        }
    }
}
