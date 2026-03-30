package com.jarvis.mobile.network

import com.google.gson.Gson
import com.jarvis.mobile.data.HistoryItem
import com.jarvis.mobile.data.NotificationEvent
import com.jarvis.mobile.data.PhoneContext
import com.jarvis.mobile.data.SavedLogin
import com.jarvis.mobile.data.StreamChunk
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class JarvisClient(var baseUrl: String) {

    /** Signed-in Google email — sent as X-User-Email header so the server knows whose account to use. */
    var googleEmail: String? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Stream a chat message to JARVIS. The response is plain text with embedded
     * <think>...</think> tags. Calls onChunk for each parsed chunk.
     * Returns the OkHttp Call so callers can cancel it.
     */
    fun streamChat(
        message: String,
        history: List<HistoryItem>,
        logins: List<SavedLogin> = emptyList(),
        phoneContext: PhoneContext? = null,
        onChunk: (StreamChunk) -> Unit
    ): Call {
        val bodyMap = buildMap<String, Any> {
            put("message", message)
            put("history", history.map { mapOf("role" to it.role, "content" to it.content) })
            put("logins", logins.map {
                mapOf("siteName" to it.siteName, "siteUrl" to it.siteUrl,
                      "username" to it.username, "password" to it.password)
            })
            if (phoneContext != null) {
                put("phoneContext", mapOf(
                    "notifications" to phoneContext.notifications.map {
                        mapOf("app" to it.app, "title" to it.title, "text" to it.text, "time" to it.time)
                    },
                    "sms" to phoneContext.sms.map {
                        mapOf("from" to it.from, "text" to it.text, "time" to it.time, "read" to it.read)
                    }
                ))
            }
        }
        val json = gson.toJson(bodyMap)
        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/chat")
            .post(body)
            .apply { googleEmail?.let { header("X-User-Email", it) } }
            .build()

        val call = httpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) {
                    onChunk(StreamChunk.Error(e.message ?: "Connection failed"))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        onChunk(StreamChunk.Error("HTTP ${resp.code}"))
                        return
                    }
                    resp.body?.use { body ->
                        // InputStreamReader handles multi-byte UTF-8 correctly across buffer boundaries
                        val reader = body.source().inputStream().reader(Charsets.UTF_8)
                        val charBuf = CharArray(1024)
                        // State machine: track <think> tag state
                        var inThink = false

                        try {
                            while (!call.isCanceled()) {
                                val n = reader.read(charBuf)
                                if (n < 0) break
                                val text = String(charBuf, 0, n)

                                // Process character by character with tag detection
                                var i = 0
                                while (i < text.length) {
                                    val remaining = text.substring(i)
                                    when {
                                        !inThink && remaining.startsWith("<think>") -> {
                                            inThink = true
                                            i += 7
                                        }
                                        inThink && remaining.startsWith("</think>") -> {
                                            inThink = false
                                            i += 8
                                            // Emit a newline after think block ends
                                            onChunk(StreamChunk.Think("\n"))
                                        }
                                        else -> {
                                            val ch = text[i].toString()
                                            if (inThink) {
                                                onChunk(StreamChunk.Think(ch))
                                            } else {
                                                onChunk(StreamChunk.Content(ch))
                                            }
                                            i++
                                        }
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            if (!call.isCanceled()) {
                                onChunk(StreamChunk.Error(e.message ?: "Stream error"))
                                return
                            }
                        }
                        if (!call.isCanceled()) {
                            onChunk(StreamChunk.Done)
                        }
                    }
                }
            }
        })

        return call
    }

    /**
     * Fetch the list of installed SAPI voices from the server.
     */
    fun fetchVoices(callback: (List<SapiVoice>) -> Unit) {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/voices")
            .get()
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(emptyList()) }
            override fun onResponse(call: Call, response: Response) {
                val json = response.use { it.body?.string() } ?: "[]"
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<SapiVoice>>() {}.type
                    callback(gson.fromJson(json, type))
                } catch (_: Exception) { callback(emptyList()) }
            }
        })
    }

    data class SapiVoice(val name: String, val gender: String, val culture: String)

    data class GoogleStatus(
        val connected: Boolean,
        val email: String? = null,
        val authUrl: String? = null,
        val reason: String? = null
    )

    fun fetchGoogleStatus(callback: (GoogleStatus) -> Unit) {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/google/status")
            .get()
            .build()
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(GoogleStatus(connected = false, reason = "unreachable"))
            }
            override fun onResponse(call: Call, response: Response) {
                val json = response.use { it.body?.string() } ?: "{}"
                try {
                    val obj = gson.fromJson(json, Map::class.java)
                    val connected = obj["connected"] as? Boolean ?: false
                    val email = obj["email"] as? String
                    val authUrl = obj["authUrl"] as? String
                    val reason = obj["reason"] as? String
                    callback(GoogleStatus(connected, email, authUrl, reason))
                } catch (_: Exception) {
                    callback(GoogleStatus(connected = false, reason = "parse_error"))
                }
            }
        })
    }

    /**
     * Fetch TTS audio from the JARVIS server.
     * Production (Linux) returns MP3 via msedge-tts; local (Windows) returns WAV via SAPI.
     * Callback receives the audio bytes and the Content-Type header so the player
     * can write the correct file extension. Returns null on any failure (TTS is best-effort).
     */
    fun fetchSpeakAudio(text: String, voice: String? = null, callback: (ByteArray?, String?) -> Unit): Call {
        val params = buildMap<String, Any?> {
            put("text", text)
            if (!voice.isNullOrBlank()) put("voice", voice)
        }
        val body = gson.toJson(params)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/speak")
            .post(body)
            .build()
        val call = httpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { callback(null, null) }
            override fun onResponse(call: Call, response: Response) {
                if (call.isCanceled()) return
                if (!response.isSuccessful) { callback(null, null); return }
                val contentType = response.header("Content-Type")
                callback(response.use { it.body?.bytes() }, contentType)
            }
        })
        return call
    }

    /**
     * Connect to the SSE notifications endpoint.
     * Returns the Call so caller can cancel when done.
     */
    fun connectNotifications(onEvent: (NotificationEvent) -> Unit): Call {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/notifications")
            .get()
            .build()

        val call = httpClient.newCall(request)

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Silent — notifications are optional, retry is handled by ViewModel
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return
                response.body?.use { body ->
                    val source = body.source()
                    try {
                        while (!source.exhausted() && !call.isCanceled()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.startsWith("data: ")) {
                                val data = line.removePrefix("data: ").trim()
                                if (data.isNotEmpty() && data != "ping") {
                                    try {
                                        val event = gson.fromJson(data, NotificationEvent::class.java)
                                        onEvent(event)
                                    } catch (_: Exception) {
                                        // Ignore malformed events
                                    }
                                }
                            }
                        }
                    } catch (_: IOException) {
                        // Connection closed
                    }
                }
            }
        })

        return call
    }
}
