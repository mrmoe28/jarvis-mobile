package com.jarvis.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.mobile.ui.theme.JarvisPurple
import kotlinx.coroutines.delay

private val ThinkBg     = Color(0x128B5CF6)  // ~7% purple
private val ThinkBorder = Color(0x408B5CF6)  // ~25% purple

/**
 * Mirrors the desktop ThinkingBubble component.
 * Shows a collapsible purple panel while JARVIS thinks.
 *
 * [isWaiting]  — request sent, zero tokens received yet  (show bouncing dots)
 * [isActive]   — currently receiving think-block tokens  (show content + timer)
 * Once both are false and [content] is non-empty, it shows "Thought for Xs" collapsed.
 */
@Composable
fun ThinkingBubble(
    content: String,
    isActive: Boolean,
    isWaiting: Boolean,
) {
    val isLive    = isActive || isWaiting
    val hasContent = content.isNotBlank()

    if (!isLive && !hasContent) return

    var expanded by remember { mutableStateOf(true) }
    var elapsed  by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    // Reset when a new thinking phase begins
    LaunchedEffect(isLive) {
        if (isLive) { elapsed = 0; expanded = true }
    }

    // Tick timer while live
    LaunchedEffect(isLive) {
        while (isLive) { delay(1000L); elapsed++ }
    }

    // Scroll to bottom as think content streams in
    LaunchedEffect(content.length) {
        if (expanded) scrollState.animateScrollTo(scrollState.maxValue)
    }

    val infinite = rememberInfiniteTransition(label = "think")
    val dotAlpha by infinite.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dot_alpha"
    )

    val topShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    val botShape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
    val fullShape = RoundedCornerShape(8.dp)
    val headerShape = if (expanded && hasContent) topShape else fullShape

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {

        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(headerShape)
                .background(ThinkBg)
                .border(1.dp, ThinkBorder, headerShape)
                .clickable(enabled = hasContent) { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pulsing status dot
            Box(
                Modifier
                    .size(6.dp)
                    .background(
                        (if (isLive) JarvisPurple else Color(0xFF4B5563))
                            .copy(alpha = if (isLive) dotAlpha else 1f),
                        CircleShape
                    )
            )
            Text(
                text = when {
                    isWaiting -> "Thinking\u2026"
                    isActive  -> "Thinking\u2026 (${elapsed}s)"
                    else      -> "Thought for ${elapsed}s"
                },
                color = if (isLive) JarvisPurple.copy(alpha = 0.85f) else Color(0xFF7C6FAA),
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            if (hasContent) {
                Text(
                    text = if (expanded) "▲" else "▼",
                    color = Color(0xFF7C6FAA),
                    fontSize = 9.sp
                )
            }
        }

        // ── Body ──────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit  = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(botShape)
                    .background(Color(0x068B5CF6))
                    .border(1.dp, ThinkBorder, botShape)
                    .heightIn(max = 200.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (isWaiting && !hasContent) {
                    // Three bouncing dots
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(3) { i ->
                            val dotAnim = rememberInfiniteTransition(label = "dot_$i")
                            val scale by dotAnim.animateFloat(
                                initialValue = 0.6f,
                                targetValue  = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    tween(400, delayMillis = i * 133),
                                    RepeatMode.Reverse
                                ),
                                label = "ds_$i"
                            )
                            Box(
                                Modifier
                                    .size(5.dp)
                                    .scale(scale)
                                    .background(Color(0xFF6D28D9), CircleShape)
                            )
                        }
                    }
                } else {
                    Text(
                        text        = content.trim(),
                        color       = Color(0xFF9CA3AF),
                        fontSize    = 11.sp,
                        fontFamily  = FontFamily.Monospace,
                        lineHeight  = 16.sp
                    )
                }
            }
        }
    }
}
