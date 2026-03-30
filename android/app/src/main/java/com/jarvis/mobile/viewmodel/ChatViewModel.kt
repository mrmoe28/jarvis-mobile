package com.jarvis.mobile.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.mobile.audio.JarvisAudioPlayer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jarvis.mobile.data.ChatMessage
import com.jarvis.mobile.data.HistoryItem
import com.jarvis.mobile.data.NotificationEvent
import com.jarvis.mobile.data.PhoneContext
import com.jarvis.mobile.data.SavedLogin
import com.jarvis.mobile.data.SearchResult
import com.jarvis.mobile.data.SourcesPayload
import com.jarvis.mobile.data.StreamChunk
import com.jarvis.mobile.notification.JarvisNotificationListener
import com.jarvis.mobile.prefs.LoginPreferences
import com.jarvis.mobile.network.JarvisClient
import com.jarvis.mobile.network.JarvisClient.SapiVoice
import com.jarvis.mobile.prefs.AppPreferences
import com.jarvis.mobile.download.JarvisDownloader
import com.jarvis.mobile.sms.SmsReader
import com.jarvis.mobile.ui.stripForSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import java.util.UUID

data class GoogleAccount(val email: String, val displayName: String)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val loginPrefs = LoginPreferences(application)
    private var client = JarvisClient(prefs.serverUrl)

    val logins = MutableStateFlow(loginPrefs.logins)

    val messages = mutableStateListOf<ChatMessage>()
    val isStreaming = MutableStateFlow(false)
    val isSpeaking  = MutableStateFlow(false)
    val serverUrl   = MutableStateFlow(prefs.serverUrl)
    val ttsEnabled  = MutableStateFlow(prefs.ttsEnabled)
    val voices      = MutableStateFlow<List<SapiVoice>>(emptyList())
    val selectedVoice = MutableStateFlow(prefs.selectedVoice)
    val googleAccount = MutableStateFlow<GoogleAccount?>(null)
    val googleAuthUrl  = MutableStateFlow<String?>(null)
    val smsPermissionGranted = MutableStateFlow(
        ContextCompat.checkSelfPermission(application, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    )

    /** Fires once when all queued TTS audio finishes — used for continuous talk trigger. */
    private val _speakingDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val speakingDone = _speakingDone.asSharedFlow()

    private var currentCall: Call? = null
    private var ttsCall: Call? = null
    private var notificationCall: Call? = null
    private var ttsSentenceBuf = StringBuilder()
    // TTS batching — accumulate 2 sentences before firing a network request
    private var ttsBatchBuf = StringBuilder()
    private var ttsBatchCount = 0
    // Count of TTS HTTP requests currently in flight — used to gate speakingDone
    private var ttsFetchActive = 0

    private val _pendingNotification = MutableStateFlow<NotificationEvent?>(null)
    val pendingNotification: StateFlow<NotificationEvent?> = _pendingNotification

    init {
        JarvisAudioPlayer.onSpeakingChanged = { playing ->
            viewModelScope.launch(Dispatchers.Main) {
                isSpeaking.value = playing
                if (!playing && !isStreaming.value && ttsFetchActive == 0) _speakingDone.tryEmit(Unit)
            }
        }
        connectNotifications()
        loadVoices()
        refreshGoogleStatus()
    }

    fun refreshGoogleStatus() {
        client.fetchGoogleStatus { status ->
            viewModelScope.launch(Dispatchers.Main) {
                if (status.connected && status.email != null) {
                    googleAccount.value = GoogleAccount(status.email, status.email)
                    client.googleEmail = status.email
                } else {
                    googleAccount.value = null
                    client.googleEmail = null
                    googleAuthUrl.value = status.authUrl
                }
            }
        }
    }

    fun loadVoices() {
        client.fetchVoices { list ->
            voices.value = list
            if (prefs.selectedVoice.isBlank() && list.isNotEmpty()) {
                prefs.selectedVoice = list[0].name
                selectedVoice.value = list[0].name
            }
        }
    }

    fun onSmsPermissionResult(granted: Boolean) {
        smsPermissionGranted.value = granted
    }

    fun setSelectedVoice(name: String) {
        prefs.selectedVoice = name
        selectedVoice.value = name
    }

    fun previewVoice(name: String) {
        client.fetchSpeakAudio("All systems online, Sir.", voice = name) { audioBytes, mimeType ->
            if (audioBytes != null) JarvisAudioPlayer.play(getApplication(), audioBytes, mimeType)
        }
    }

    /** Stop TTS audio immediately. Cancels any in-flight TTS network request too. */
    fun stopSpeaking() {
        ttsCall?.cancel()
        ttsCall = null
        ttsFetchActive = 0
        JarvisAudioPlayer.stop()
        isSpeaking.value = false
    }

    /** Interrupt — cancel any in-flight response AND stop audio. */
    fun interrupt() {
        cancelStreaming()
        stopSpeaking()
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (isStreaming.value) return

        JarvisAudioPlayer.stop()
        ttsSentenceBuf = StringBuilder()
        ttsBatchBuf = StringBuilder()
        ttsBatchCount = 0
        ttsFetchActive = 0

        val userMsg = ChatMessage(role = "user", content = text.trim())
        messages.add(userMsg)

        val assistantId = UUID.randomUUID().toString()
        messages.add(ChatMessage(id = assistantId, role = "assistant", content = "", isStreaming = true))
        isStreaming.value = true

        val history = messages
            .filter { !it.isStreaming }
            .dropLast(1)
            .map { HistoryItem(it.role, it.content) }

        var thinkBuf = StringBuilder()
        var contentBuf = StringBuilder()

        val notifications = synchronized(JarvisNotificationListener.recent) {
            JarvisNotificationListener.recent.toList()
        }
        val sms = if (smsPermissionGranted.value) {
            SmsReader.read(getApplication())
        } else emptyList()
        val phoneContext = if (notifications.isNotEmpty() || sms.isNotEmpty()) {
            PhoneContext(notifications = notifications, sms = sms)
        } else null

        currentCall = client.streamChat(
            message = text.trim(),
            history = history,
            logins = loginPrefs.logins,
            phoneContext = phoneContext,
            onChunk = { chunk ->
                viewModelScope.launch(Dispatchers.Main) {
                    val idx = messages.indexOfFirst { it.id == assistantId }
                    if (idx < 0) return@launch

                    when (chunk) {
                        is StreamChunk.Think -> {
                            thinkBuf.append(chunk.text)
                            messages[idx] = messages[idx].copy(thinkContent = thinkBuf.toString())
                        }
                        is StreamChunk.Content -> {
                            contentBuf.append(chunk.text)
                            messages[idx] = messages[idx].copy(content = contentBuf.toString())
                            // Batch 2 sentences per TTS request — halves network round-trips
                            if (prefs.ttsEnabled) {
                                ttsSentenceBuf.append(chunk.text)
                                val boundary = findSentenceBoundary(ttsSentenceBuf.toString())
                                if (boundary >= 0) {
                                    val sentence = ttsSentenceBuf.substring(0, boundary + 1).trim()
                                    ttsSentenceBuf = StringBuilder(ttsSentenceBuf.substring(boundary + 1))
                                    if (sentence.isNotBlank()) {
                                        ttsBatchBuf.append(sentence).append(" ")
                                        ttsBatchCount++
                                        if (ttsBatchCount >= 2) {
                                            speakSegment(ttsBatchBuf.toString().trim())
                                            ttsBatchBuf = StringBuilder()
                                            ttsBatchCount = 0
                                        }
                                    }
                                }
                            }
                        }
                        is StreamChunk.Done -> {
                            val full = contentBuf.toString()
                            val sources = parseSources(full)
                            triggerDownloads(full)
                            val cleanContent = full
                                .replace(Regex("""<!--SOURCES:[\s\S]+?-->"""), "")
                                .replace(Regex("""<!--DOWNLOAD:[\s\S]+?-->"""), "")
                                .trim()
                            messages[idx] = messages[idx].copy(
                                content = cleanContent,
                                isStreaming = false,
                                sources = sources
                            )
                            isStreaming.value = false
                            // Flush batch + any remaining unbounded text in one TTS call
                            if (prefs.ttsEnabled) {
                                val tail = ttsSentenceBuf.toString()
                                    .replace(Regex("""<!--SOURCES:[\s\S]+?-->"""), "").trim()
                                val flush = (ttsBatchBuf.toString().trim() + " " + tail).trim()
                                if (flush.isNotBlank()) speakSegment(flush)
                                ttsSentenceBuf = StringBuilder()
                                ttsBatchBuf = StringBuilder()
                                ttsBatchCount = 0
                            }
                        }
                        is StreamChunk.Error -> {
                            messages[idx] = messages[idx].copy(
                                content = "Error: ${chunk.message}",
                                isStreaming = false
                            )
                            isStreaming.value = false
                        }
                    }
                }
            }
        )
    }

    private fun findSentenceBoundary(text: String): Int {
        if (text.length < 20) return -1
        for (i in text.indices.reversed()) {
            val ch = text[i]
            if (ch == '.' || ch == '!' || ch == '?') {
                val next = text.getOrNull(i + 1)
                if (next == ' ' || next == '\n' || next == null) {
                    val prev = text.getOrNull(i - 1)
                    if (prev != null && prev.isDigit()) continue
                    return i
                }
            }
            if (ch == '\n' && text.getOrNull(i - 1) == '\n') return i
        }
        if (text.length > 200) {
            val lastSpace = text.lastIndexOf(' ', 199)
            if (lastSpace > 80) return lastSpace
        }
        return -1
    }

    private fun speakSegment(text: String) {
        val clean = stripForSpeech(text)
        if (clean.isBlank()) return
        ttsFetchActive++
        ttsCall = client.fetchSpeakAudio(clean, voice = prefs.selectedVoice.ifBlank { null }) { audioBytes, mimeType ->
            viewModelScope.launch(Dispatchers.Main) {
                ttsFetchActive--
                if (audioBytes != null) {
                    JarvisAudioPlayer.enqueue(getApplication(), audioBytes, mimeType)
                } else if (!isStreaming.value && ttsFetchActive == 0 && !isSpeaking.value) {
                    // TTS fetch failed and nothing else is playing — signal done
                    _speakingDone.tryEmit(Unit)
                }
            }
        }
    }

    fun cancelStreaming() {
        currentCall?.cancel()
        isStreaming.value = false
        JarvisAudioPlayer.stop()
        val idx = messages.indexOfLast { it.isStreaming }
        if (idx >= 0) {
            messages[idx] = messages[idx].copy(isStreaming = false)
        }
    }

    fun updateServerUrl(url: String) {
        val clean = url.trim().let {
            if (it.isNotEmpty() && !it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
        }
        prefs.serverUrl = clean
        serverUrl.value = clean
        client = JarvisClient(clean)
        notificationCall?.cancel()
        connectNotifications()
        loadVoices()
    }

    fun setTtsEnabled(enabled: Boolean) {
        prefs.ttsEnabled = enabled
        ttsEnabled.value = enabled
        if (!enabled) JarvisAudioPlayer.stop()
    }

    fun newChat() {
        interrupt()
        messages.clear()
    }

    fun clearNotification() {
        _pendingNotification.value = null
    }

    private fun connectNotifications() {
        notificationCall = client.connectNotifications { event ->
            viewModelScope.launch(Dispatchers.Main) {
                _pendingNotification.value = event
                if (event.type == "jarvis_proactive" && event.content.isNotBlank()) {
                    messages.add(ChatMessage(role = "assistant", content = event.content))
                    if (prefs.ttsEnabled) {
                        ttsCall = client.fetchSpeakAudio(event.content, voice = prefs.selectedVoice.ifBlank { null }) { audioBytes, mimeType ->
                            if (audioBytes != null) JarvisAudioPlayer.play(getApplication(), audioBytes, mimeType)
                        }
                    }
                } else if (event.label.isNotBlank()) {
                    messages.add(
                        ChatMessage(
                            role = "assistant",
                            content = "Task complete: **${event.label}**\n${event.result.take(200).ifEmpty { "" }}".trim()
                        )
                    )
                    if (prefs.ttsEnabled) {
                        ttsCall = client.fetchSpeakAudio("Task complete: ${event.label}", voice = prefs.selectedVoice.ifBlank { null }) { audioBytes, mimeType ->
                            if (audioBytes != null) JarvisAudioPlayer.play(getApplication(), audioBytes, mimeType)
                        }
                    }
                }
            }
        }
    }

    fun saveLogin(login: SavedLogin) {
        loginPrefs.save(login)
        logins.value = loginPrefs.logins
    }

    fun deleteLogin(id: String) {
        loginPrefs.delete(id)
        logins.value = loginPrefs.logins
    }

    private fun parseSources(raw: String): List<SearchResult> {
        val match = Regex("""<!--SOURCES:([A-Za-z0-9+/=]+)-->""").find(raw) ?: return emptyList()
        return try {
            val json = String(android.util.Base64.decode(match.groupValues[1], android.util.Base64.DEFAULT))
            val type = object : TypeToken<SourcesPayload>() {}.type
            val payload: SourcesPayload = Gson().fromJson(json, type)
            payload.results
        } catch (_: Exception) { emptyList() }
    }

    private fun triggerDownloads(raw: String) {
        Regex("""<!--DOWNLOAD:([A-Za-z0-9+/=]+)-->""").findAll(raw).forEach { match ->
            try {
                val json = String(android.util.Base64.decode(match.groupValues[1], android.util.Base64.DEFAULT))
                val obj = Gson().fromJson(json, Map::class.java)
                val url      = obj["url"] as? String ?: return@forEach
                val filename = obj["filename"] as? String ?: "download"
                JarvisDownloader.download(getApplication(), url, filename)
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentCall?.cancel()
        ttsCall?.cancel()
        notificationCall?.cancel()
        JarvisAudioPlayer.stop()
    }
}
