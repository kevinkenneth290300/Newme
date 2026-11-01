package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.model.ThemeMode

val LocalReducedMotion = staticCompositionLocalOf { false }

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF78DCB0), onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF174C39), onPrimaryContainer = Color(0xFFACF2D3),
    secondary = Color(0xFF80D5DD), onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF164D55), onSecondaryContainer = Color(0xFFB2EEF2),
    tertiary = Color(0xFFF4B3BA), onTertiary = Color(0xFF511F29),
    tertiaryContainer = Color(0xFF6F3540), onTertiaryContainer = Color(0xFFFFDADF),
    background = Color(0xFF111615), onBackground = Color(0xFFE2E9E5),
    surface = Color(0xFF171E1C), onSurface = Color(0xFFE2E9E5),
    surfaceVariant = Color(0xFF2E3B36), onSurfaceVariant = Color(0xFFC3CEC7),
    surfaceContainerLowest = Color(0xFF0C1110), surfaceContainerLow = Color(0xFF1B2320),
    surfaceContainer = Color(0xFF202925), surfaceContainerHigh = Color(0xFF27322D),
    surfaceContainerHighest = Color(0xFF303D36),
    outline = Color(0xFF89968E), outlineVariant = Color(0xFF45544A),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF096C48), onPrimary = Color.White,
    primaryContainer = Color(0xFFCEF5E1), onPrimaryContainer = Color(0xFF003826),
    secondary = Color(0xFF006877), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3F0F3), onSecondaryContainer = Color(0xFF00363D),
    tertiary = Color(0xFF8A394A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDADF), onTertiaryContainer = Color(0xFF511F29),
    background = Color(0xFFF6F8F7), onBackground = Color(0xFF19231F),
    surface = Color.White, onSurface = Color(0xFF19231F),
    surfaceVariant = Color(0xFFE2EBE6), onSurfaceVariant = Color(0xFF46564D),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F5F3),
    surfaceContainer = Color(0xFFEBF1EE), surfaceContainerHigh = Color(0xFFE5ECE8),
    surfaceContainerHighest = Color(0xFFDFE7E2),
    outline = Color(0xFF6E8074), outlineVariant = Color(0xFFBCCBC1),
    error = Color(0xFFBA1A1A), onError = Color.White, errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NewMeTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

@Composable
fun NewMeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) { ThemeMode.SYSTEM -> darkTheme; ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion) {
        MaterialTheme(colorScheme = if (dark) DarkColorScheme else LightColorScheme, typography = Typography, content = content)
    }
}

