package com.jarvis.mobile.data

import java.util.UUID

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val image: String? = null
)

data class SourcesPayload(
    val query: String,
    val results: List<SearchResult>
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val thinkContent: String = "",
    val isStreaming: Boolean = false,
    val sources: List<SearchResult> = emptyList()
)

data class SavedLogin(
    val id: String = UUID.randomUUID().toString(),
    val siteName: String = "",
    val siteUrl: String = "",
    val username: String = "",
    val password: String = ""
)

data class HistoryItem(
    val role: String,
    val content: String
)

sealed class StreamChunk {
    data class Think(val text: String) : StreamChunk()
    data class Content(val text: String) : StreamChunk()
    object Done : StreamChunk()
    data class Error(val message: String) : StreamChunk()
}

data class NotificationEvent(
    val type: String = "",
    val label: String = "",
    val result: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)

data class PhoneNotification(
    val app: String,
    val title: String,
    val text: String,
    val time: Long
)

data class PhoneSms(
    val from: String,
    val text: String,
    val time: Long,
    val read: Boolean
)

data class PhoneContext(
    val notifications: List<PhoneNotification>,
    val sms: List<PhoneSms>
)
