package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NewMePrimary,
    onPrimary = NewMePrimaryDark,
    primaryContainer = NewMePrimaryContainer,
    onPrimaryContainer = NewMePrimary,
    secondary = NewMeSecondary,
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = NewMeTertiary,
    onTertiary = Color(0xFF00363A),
    tertiaryContainer = Color(0xFF004F54),
    onTertiaryContainer = Color(0xFF80D8FF),
    background = NewMeBackground,
    onBackground = NewMeTextPrimary,
    surface = NewMeSurface,
    onSurface = NewMeTextPrimary,
    surfaceVariant = NewMeSurfaceVariant,
    onSurfaceVariant = NewMeTextSecondary,
    outline = NewMeBorder,
    outlineVariant = NewMeBorderStrong,
    error = NewMeCoral,
    onError = Color(0xFF601410)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Force consistent athletic dark OS theme
    content: @Composable () -> Unit
) {
    // NewMe is a dark-first serious personal transformation OS
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun NewMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

