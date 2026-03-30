package com.jarvis.mobile.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.mobile.audio.SpeechInput
import com.jarvis.mobile.ui.theme.JarvisBlue
import com.jarvis.mobile.ui.theme.JarvisPurple
import com.jarvis.mobile.ui.theme.Surface3
import com.jarvis.mobile.ui.theme.TextMuted
import com.jarvis.mobile.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

@Composable
fun VoicePage(
    vm: ChatViewModel,
    speech: SpeechInput,
    onBack: () -> Unit
) {
    val isSpeaking  by vm.isSpeaking.collectAsState()
    val isStreaming  by vm.isStreaming.collectAsState()

    var isListening  by remember { mutableStateOf(false) }
    var isContinuous by remember { mutableStateOf(false) }

    fun startListening() {
        if (isListening) return
        isListening = true
        speech.startListening(
            onReady         = {},
            onSpeechStarted = {},
            onResult = { transcript ->
                isListening = false
                if (transcript.isNotBlank()) vm.sendMessage(transcript)
            },
            onError = { isListening = false }
        )
    }

    fun stopListening() {
        speech.stopListening()
        isListening = false
    }

    // Continuous mode: auto-listen after JARVIS finishes speaking
    LaunchedEffect(Unit) {
        vm.speakingDone.collect {
            if (isContinuous && !isStreaming && !isSpeaking && !isListening) {
                delay(350L)
                if (isContinuous && !isStreaming && !isSpeaking && !isListening) startListening()
            }
        }
    }

    val isBusy = isSpeaking || isStreaming

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C12))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chat",
                        tint               = TextMuted,
                        modifier           = Modifier.size(20.dp)
                    )
                }
                Text(
                    text  = "J A R V I S",
                    style = TextStyle(
                        brush       = Brush.horizontalGradient(listOf(JarvisBlue, JarvisPurple)),
                        fontSize    = 14.sp,
                        letterSpacing = 5.sp,
                        fontFamily  = FontFamily.Default
                    )
                )
                // Placeholder to balance the row
                Box(Modifier.size(48.dp))
            }

            // ── Orb — fills all available space ────────────────────────────
            Box(
                modifier         = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                VoiceOrb(
                    isSpeaking  = isSpeaking,
                    isListening = isListening,
                    isStreaming  = isStreaming,
                    onTap       = {
                        when {
                            isBusy      -> { vm.interrupt(); startListening() }
                            isListening -> stopListening()
                            else        -> startListening()
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Continuous mode toggle ─────────────────────────────────────
            Row(
                modifier            = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isContinuous) JarvisPurple.copy(alpha = 0.2f) else Color(0xFF111827),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) {
                            isContinuous = !isContinuous
                            if (!isContinuous) stopListening()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Autorenew,
                        contentDescription = "Continuous mode",
                        tint     = if (isContinuous) JarvisPurple else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ── Voice Orb ─────────────────────────────────────────────────────────────────

@Composable
private fun VoiceOrb(
    isSpeaking: Boolean,
    isListening: Boolean,
    isStreaming: Boolean,
    onTap: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "voiceOrb")

    // Idle slow breathe — always alive
    val idleBreathe by transition.animateFloat(
        0.97f, 1.03f,
        infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "idle"
    )

    // Speaking pulse — faster and more pronounced
    val speakPulse by transition.animateFloat(
        0.88f, 1.14f,
        infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "speak"
    )

    // Listening gentle pulse
    val listenPulse by transition.animateFloat(
        0.93f, 1.07f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        "listen"
    )

    // 3 ripple rings — staggered
    val r1 by transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart, StartOffset(0)), "r1")
    val r2 by transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart, StartOffset(600)), "r2")
    val r3 by transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart, StartOffset(1200)), "r3")

    val coreScale = when {
        isSpeaking  -> speakPulse
        isListening -> listenPulse
        else        -> idleBreathe
    }

    // Ring size: core at 130dp, expanding to 310dp
    val ringMin = 130f
    val ringMax = 310f

    Box(
        modifier = Modifier
            .size(320.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ripple rings — white, only when active
        if (isSpeaking || isListening) {
            listOf(r1, r2, r3).forEach { r ->
                val ringSize = ringMin + (ringMax - ringMin) * r
                Box(
                    Modifier
                        .size(ringSize.dp)
                        .background(
                            Color.White.copy(alpha = (1f - r) * 0.12f),
                            CircleShape
                        )
                )
            }
        }

        // Outermost soft glow
        Box(
            Modifier
                .size(200.dp)
                .scale(coreScale)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Mid glow
        Box(
            Modifier
                .size(150.dp)
                .scale(coreScale)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Inner glow
        Box(
            Modifier
                .size(110.dp)
                .scale(coreScale)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // Solid white core
        Box(
            Modifier
                .size(80.dp)
                .scale(coreScale)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.92f)
                        )
                    ),
                    CircleShape
                )
        )
    }
}
