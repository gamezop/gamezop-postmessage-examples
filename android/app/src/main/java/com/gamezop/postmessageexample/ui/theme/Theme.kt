package com.gamezop.postmessageexample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4CF0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E2FF),
    onPrimaryContainer = Color(0xFF201566),
    secondary = Color(0xFF006B5B),
    secondaryContainer = Color(0xFF74F8DA),
    background = Color(0xFFF7F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EAF2),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC8BFFF),
    primaryContainer = Color(0xFF4235D1),
    secondary = Color(0xFF53DBC0),
    background = Color(0xFF0B1020),
    surface = Color(0xFF13182A),
    surfaceVariant = Color(0xFF282D40),
)

@Composable
fun GamezopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

