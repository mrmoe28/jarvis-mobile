package com.jarvis.mobile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jarvis.mobile.audio.SpeechInput
import com.jarvis.mobile.data.ChatMessage
import com.jarvis.mobile.data.SearchResult
import com.jarvis.mobile.ui.theme.JarvisBlue
import com.jarvis.mobile.ui.theme.JarvisPurple
import com.jarvis.mobile.ui.theme.Surface1
import com.jarvis.mobile.ui.theme.Surface2
import com.jarvis.mobile.ui.theme.Surface3
import com.jarvis.mobile.ui.theme.TextMuted
import com.jarvis.mobile.ui.theme.TextSecondary
import com.jarvis.mobile.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val messages      = vm.messages
    val isStreaming   by vm.isStreaming.collectAsState()
    val isSpeaking    by vm.isSpeaking.collectAsState()
    val listState     = rememberLazyListState()
    var input         by remember { mutableStateOf("") }
    var showSettings  by remember { mutableStateOf(false) }
    var isListening      by remember { mutableStateOf(false) }
    var isContinuous     by remember { mutableStateOf(false) }
    var voiceMode        by remember { mutableStateOf(false) }
    var autoInterrupting by remember { mutableStateOf(false) }
    val keyboard      = LocalSoftwareKeyboardController.current
    val context       = LocalContext.current
    val speech        = remember { SpeechInput(context) }

    DisposableEffect(Unit) { onDispose { speech.destroy() } }

    // Show voice page as full-screen overlay
    if (voiceMode) {
        VoicePage(
            vm      = vm,
            speech  = speech,
            onBack  = { voiceMode = false }
        )
        return
    }

    // ── Listening helpers ─────────────────────────────────────────────────────
    fun startListening(onSpeechStarted: () -> Unit = {}) {
        if (isListening) return
        isListening = true
        speech.startListening(
            onReady         = {},
            onSpeechStarted = onSpeechStarted,
            onResult = { transcript ->
                isListening = false
                autoInterrupting = false
                if (transcript.isNotBlank()) vm.sendMessage(transcript)
            },
            onError  = { isListening = false; autoInterrupting = false }
        )
    }

    fun stopListening() {
        speech.stopListening()
        isListening = false
    }

    // ── Auto-scroll ───────────────────────────────────────────────────────────
    LaunchedEffect(messages.size, isStreaming) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // ── Continuous talk ───────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        vm.speakingDone.collect {
            if (isContinuous && !isStreaming && !isListening) {
                delay(400L)
                if (isContinuous && !isStreaming && !isListening) startListening()
            }
        }
    }

    // ── Auto-interrupt: arm mic when JARVIS speaks so talking stops him instantly ──
    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            if (!isListening) {
                autoInterrupting = true
                startListening(onSpeechStarted = {
                    autoInterrupting = false
                    vm.interrupt()
                })
            }
        } else if (autoInterrupting) {
            autoInterrupting = false
            stopListening()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "J A R V I S",
                style = TextStyle(
                    brush = Brush.horizontalGradient(listOf(JarvisBlue, JarvisPurple)),
                    fontSize = 16.sp,
                    letterSpacing = 5.sp,
                    fontFamily = FontFamily.Default
                )
            )
            Row {
                IconButton(onClick = { vm.newChat() }) {
                    Icon(Icons.Default.Add, "New chat", tint = TextMuted, modifier = Modifier.size(20.dp))
                }
                // Voice mode button
                IconButton(onClick = { voiceMode = true }) {
                    Icon(Icons.Default.GraphicEq, "Voice mode", tint = TextMuted, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, "Settings", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Surface2))

        // ── Messages ──────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            if (messages.isEmpty() && !isStreaming) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "J A R V I S",
                            style = TextStyle(
                                brush = Brush.horizontalGradient(listOf(JarvisBlue, JarvisPurple)),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 8.sp
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Just A Rather Very Intelligent System",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                if (msg.role == "user") UserBubble(msg)
                else AssistantContent(msg)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Surface2))

        // ── Input bar ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface1)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value         = input,
                onValueChange = { input = it },
                modifier      = Modifier.weight(1f),
                placeholder   = {
                    Text(
                        text = when {
                            isStreaming  -> "JARVIS is responding…"
                            isSpeaking   -> "JARVIS is speaking…"
                            isContinuous -> "Continuous mode…"
                            else         -> "Message JARVIS…"
                        },
                        color    = TextMuted,
                        fontSize = 14.sp
                    )
                },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = Surface3,
                    unfocusedBorderColor    = Surface3,
                    cursorColor             = JarvisBlue,
                    focusedContainerColor   = Surface1,
                    unfocusedContainerColor = Surface1,
                    disabledBorderColor     = Surface2,
                    disabledContainerColor  = Surface1,
                ),
                shape    = RoundedCornerShape(24.dp),
                enabled  = !isStreaming && !isSpeaking,
                singleLine = false,
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank() && !isStreaming && !isSpeaking) {
                        vm.sendMessage(input.trim())
                        input = ""
                        keyboard?.hide()
                    }
                })
            )

            Spacer(Modifier.width(8.dp))

            val isBusy = isStreaming || isSpeaking
            IconButton(
                onClick = {
                    when {
                        isBusy             -> { vm.interrupt(); startListening() }
                        input.isNotBlank() -> {
                            vm.sendMessage(input.trim())
                            input = ""
                            keyboard?.hide()
                        }
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        when {
                            isStreaming -> Color(0xFF7F1D1D)
                            isSpeaking  -> Color(0xFF7C3A00)
                            else        -> JarvisBlue
                        },
                        CircleShape
                    )
            ) {
                Icon(
                    if (isBusy) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (isBusy) "Stop" else "Send",
                    tint     = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            MicButton(
                isListening = isListening,
                onToggle    = {
                    when {
                        isStreaming || isSpeaking -> { vm.interrupt(); startListening() }
                        isListening              -> stopListening()
                        else                     -> startListening()
                    }
                }
            )

            Spacer(Modifier.width(4.dp))

            // Continuous toggle
            IconButton(
                onClick  = { isContinuous = !isContinuous; if (!isContinuous) stopListening() },
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isContinuous) JarvisPurple.copy(alpha = 0.25f) else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Autorenew,
                    contentDescription = "Continuous talk",
                    tint     = if (isContinuous) JarvisPurple else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState       = rememberModalBottomSheetState(),
            containerColor   = Surface1,
            tonalElevation   = 0.dp
        ) {
            SettingsSheet(vm = vm, onDismiss = { showSettings = false })
        }
    }
}

// ── AssistantContent ──────────────────────────────────────────────────────────

@Composable
private fun AssistantContent(msg: ChatMessage) {
    val isWaiting        = msg.isStreaming && msg.content.isEmpty() && msg.thinkContent.isEmpty()
    val isThinkingActive = msg.isStreaming && msg.thinkContent.isNotBlank() && msg.content.isEmpty()
    val hasThink         = msg.thinkContent.isNotBlank()

    if (isWaiting || hasThink) {
        ThinkingBubble(content = msg.thinkContent, isActive = isThinkingActive, isWaiting = isWaiting)
    }
    if (msg.content.isNotBlank()) AssistantBubble(msg)
    if (msg.sources.isNotEmpty()) SearchSourceCards(msg.sources)
}

// ── User bubble ───────────────────────────────────────────────────────────────

@Composable
private fun UserBubble(msg: ChatMessage) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    Color(0xFF1A2744),
                    RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text = msg.content, color = Color(0xFFBFD7FD), fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

// ── Assistant bubble ──────────────────────────────────────────────────────────

@Composable
private fun AssistantBubble(msg: ChatMessage) {
    val infinite = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infinite.animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse),
        "ca"
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        val inlineImages = extractImages(msg.content)
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    Surface2,
                    RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text       = buildRichAnnotatedString(msg.content, JarvisBlue),
                color      = MaterialTheme.colorScheme.onSurface,
                fontSize   = 14.sp,
                lineHeight = 20.sp
            )
            inlineImages.forEach { imgUrl ->
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model              = imgUrl,
                    contentDescription = "screenshot",
                    contentScale       = ContentScale.FillWidth,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface1)
                )
            }
            if (msg.isStreaming) {
                Text("▌", color = JarvisBlue.copy(alpha = cursorAlpha), fontSize = 14.sp)
            }
        }
    }
}

// ── Search source cards ───────────────────────────────────────────────────────

@Composable
private fun SearchSourceCards(sources: List<SearchResult>) {
    Column {
        Text(
            text = "Sources",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(sources, key = { _, s -> s.url }) { index, source ->
                SearchCard(source, index + 1)
            }
        }
    }
}

@Composable
private fun SearchCard(source: SearchResult, index: Int) {
    val context = LocalContext.current
    val domain = runCatching { java.net.URI(source.url).host?.removePrefix("www.") ?: "" }.getOrDefault("")
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) },
        colors = CardDefaults.cardColors(containerColor = Surface2),
        shape  = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Image area with numbered badge overlay
            Box {
                if (!source.image.isNullOrBlank()) {
                    AsyncImage(
                        model              = source.image,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxWidth().height(90.dp)
                    )
                } else {
                    Box(Modifier.fillMaxWidth().height(90.dp).background(Surface1))
                }
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp)
                        .background(JarvisBlue.copy(alpha = 0.9f), CircleShape)
                        .align(Alignment.TopStart),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$index", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.padding(8.dp)) {
                // Domain row with favicon
                if (domain.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model              = "https://www.google.com/s2/favicons?domain=$domain&sz=16",
                            contentDescription = null,
                            modifier           = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(domain, color = TextMuted, fontSize = 9.sp, maxLines = 1)
                    }
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    text       = source.title,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp,
                    maxLines   = 2,
                    overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (source.snippet.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text       = source.snippet,
                        color      = TextMuted,
                        fontSize   = 9.sp,
                        lineHeight = 12.sp,
                        maxLines   = 2,
                        overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ── Mic button ────────────────────────────────────────────────────────────────

@Composable
private fun MicButton(isListening: Boolean, onToggle: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "mic")
    val scale by infinite.animateFloat(
        1f, if (isListening) 1.15f else 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse),
        "mic_scale"
    )

    IconButton(
        onClick  = onToggle,
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .background(if (isListening) JarvisPurple else Surface2, CircleShape)
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "Voice input",
            tint     = if (isListening) Color.White else TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}
