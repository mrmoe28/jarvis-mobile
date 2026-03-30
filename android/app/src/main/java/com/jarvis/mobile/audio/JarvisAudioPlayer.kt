package com.jarvis.mobile.audio

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.ArrayDeque

/**
 * Plays audio bytes returned by the JARVIS server's /api/speak endpoint.
 * Supports MP3 (production/Linux via msedge-tts) and WAV (local/Windows via SAPI).
 *
 * Supports two modes:
 *  - play()    — interrupts everything and plays a single clip (used for voice preview)
 *  - enqueue() — adds a clip to the sequential playback queue (used for sentence streaming)
 *
 * onSpeakingChanged fires with true when playback starts and false when the queue drains.
 */
object JarvisAudioPlayer {

    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null

    private data class AudioClip(val bytes: ByteArray, val mimeType: String?)
    private val queue = ArrayDeque<AudioClip>()

    /** Called with true when audio starts playing, false when all audio stops. */
    var onSpeakingChanged: ((Boolean) -> Unit)? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /** Add audio bytes to the playback queue. Starts playing immediately if idle. */
    fun enqueue(context: Context, audioBytes: ByteArray, mimeType: String? = null) {
        main.post {
            queue.addLast(AudioClip(audioBytes, mimeType))
            if (player == null) playNext(context)
        }
    }

    /** Interrupt any current playback and play a single clip (e.g. voice preview). */
    fun play(context: Context, audioBytes: ByteArray, mimeType: String? = null, onComplete: (() -> Unit)? = null) {
        main.post {
            stopInternal()
            startPlayer(context, audioBytes, mimeType, onComplete)
        }
    }

    /** Stop all playback and clear the queue. */
    fun stop() {
        main.post { stopInternal() }
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun playNext(context: Context) {
        val clip = queue.pollFirst()
        if (clip == null) {
            onSpeakingChanged?.invoke(false)
            return
        }
        startPlayer(context, clip.bytes, clip.mimeType) { playNext(context) }
    }

    private fun startPlayer(context: Context, audioBytes: ByteArray, mimeType: String?, onComplete: (() -> Unit)?) {
        try {
            val ext = if (mimeType?.contains("mpeg") == true) ".mp3" else ".wav"
            val tmp = File.createTempFile("jarvis_tts_", ext, context.cacheDir)
            tmp.writeBytes(audioBytes)
            val mp = MediaPlayer()
            player = mp
            onSpeakingChanged?.invoke(true)
            mp.setDataSource(tmp.absolutePath)
            mp.setOnCompletionListener {
                it.release()
                if (player === it) player = null
                tmp.delete()
                onComplete?.invoke()
            }
            mp.setOnErrorListener { it, what, extra ->
                android.util.Log.e("JarvisAudioPlayer", "MediaPlayer error: what=$what extra=$extra mimeType=$mimeType")
                it.release()
                if (player === it) player = null
                tmp.delete()
                onComplete?.invoke()
                true
            }
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            android.util.Log.e("JarvisAudioPlayer", "startPlayer failed: ${e.message}")
            player = null
            onSpeakingChanged?.invoke(false)
            onComplete?.invoke()
        }
    }

    private fun stopInternal() {
        queue.clear()
        player?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }
        player = null
        onSpeakingChanged?.invoke(false)
    }
}
