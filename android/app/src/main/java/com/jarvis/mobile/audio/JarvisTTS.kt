package com.jarvis.mobile.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

class JarvisTTS private constructor(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    /** Callback fired when the engine is ready and voices are loaded. */
    var onReady: ((List<AndroidVoice>) -> Unit)? = null

    data class AndroidVoice(val name: String, val locale: String, val quality: Int)

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(0.95f)
                isReady = true
                val voices = getAvailableVoices()
                onReady?.invoke(voices)
            }
        }
    }

    fun getAvailableVoices(): List<AndroidVoice> {
        return tts?.voices
            ?.filter { !it.isNetworkConnectionRequired && it.locale.language == "en" }
            ?.sortedByDescending { it.quality }
            ?.map { AndroidVoice(it.name, it.locale.toLanguageTag(), it.quality) }
            ?: emptyList()
    }

    fun setVoice(voiceName: String) {
        if (!isReady) return
        val voice = tts?.voices?.find { it.name == voiceName }
        if (voice != null) tts?.voice = voice
    }

    fun speak(text: String) {
        if (!isReady) return
        val cleaned = toSpeech(text)
        if (cleaned.isBlank()) return
        tts?.speak(cleaned, TextToSpeech.QUEUE_ADD, null, "jarvis_${System.currentTimeMillis()}")
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking == true

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        instance = null
    }

    /** Port of the desktop toSpeech() function — strips markdown before speaking. */
    private fun toSpeech(text: String): String = text
        .replace(Regex("<think>[\\s\\S]*?</think>"), "")
        .replace(Regex("```[\\s\\S]*?```"), "... I've written the code for you, Sir, check the chat.")
        .replace(Regex("`([^`]+)`"), "$1")
        .replace(Regex("\\*{1,3}([^*]+)\\*{1,3}"), "$1")
        .replace(Regex("(?m)^#{1,6}\\s+"), "")
        .replace(Regex("https?://[^\\s)>]+"), "")
        .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
        .replace(Regex("(?m)^\\s*[-*•]\\s+"), "... ")
        .replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "... ")
        .replace(Regex("<[^>]+>"), "")
        .replace(Regex("[#|>_~^]"), "")
        .replace("────────────────────", "")
        .replace(Regex("(\\.\\.\\.\\s*)+"), "... ")
        .replace(Regex("\\n{2,}"), "... ")
        .replace("\n", " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

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
