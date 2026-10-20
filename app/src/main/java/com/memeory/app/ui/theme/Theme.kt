package com.memeory.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Playful but clean: a bold purple with a "meme yellow" accent.
private val LightColors = lightColorScheme(
    primary = Color(0xFF6A4CF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DEFF),
    onPrimaryContainer = Color(0xFF1F0A6B),
    secondary = Color(0xFF7A5900),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFFC2185B),
    background = Color(0xFFFCFAFF),
    surface = Color(0xFFFCFAFF),
    surfaceVariant = Color(0xFFE7E1F2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BDFF),
    onPrimary = Color(0xFF30178F),
    primaryContainer = Color(0xFF4A31C9),
    onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = Color(0xFFFFD45E),
    onSecondary = Color(0xFF3F2E00),
    secondaryContainer = Color(0xFF5B4300),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = Color(0xFFFFB0C8),
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
    surfaceVariant = Color(0xFF2B2733),
)

@Composable
fun MemeoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
