package com.jarvis.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val JarvisBlue   = Color(0xFF4285F4)
val JarvisPurple = Color(0xFF8B5CF6)
val JarvisCyan   = Color(0xFF06B6D4)
val Surface0     = Color(0xFF080808)
val Surface1     = Color(0xFF0A0A0A)
val Surface2     = Color(0xFF111111)
val Surface3     = Color(0xFF1A1A1A)
val TextPrimary  = Color(0xFFE8EAED)
val TextSecondary= Color(0xFF9AA0A6)
val TextMuted    = Color(0xFF4B5563)

private val JarvisColors = darkColorScheme(
    primary          = JarvisBlue,
    secondary        = JarvisPurple,
    tertiary         = JarvisCyan,
    background       = Surface0,
    surface          = Surface1,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onPrimary        = Color.White,
    surfaceVariant   = Surface2,
    outlineVariant   = Surface3,
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColors,
        content = content
    )
}
