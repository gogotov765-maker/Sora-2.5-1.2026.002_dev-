package com.sora25.app2.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF8A5CFF)
val AccentPink = Color(0xFFFF5FA2)
val Bg = Color(0xFF0B0B14)
val Card = Color(0xFF171726)

@Composable
fun Sora25Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            secondary = AccentPink,
            background = Bg,
            surface = Card,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}
