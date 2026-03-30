package com.jarvis.mobile.prefs

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var selectedVoice: String
        get() = prefs.getString(KEY_SELECTED_VOICE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SELECTED_VOICE, value).apply()

    var googleEmail: String
        get() = prefs.getString(KEY_GOOGLE_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_EMAIL, value).apply()

    var googleDisplayName: String
        get() = prefs.getString(KEY_GOOGLE_DISPLAY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GOOGLE_DISPLAY_NAME, value).apply()

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_SELECTED_VOICE = "selected_voice"
        private const val KEY_GOOGLE_EMAIL = "google_email"
        private const val KEY_GOOGLE_DISPLAY_NAME = "google_display_name"
        const val DEFAULT_SERVER_URL = "https://jarvis.lock28.com"
    }
}
