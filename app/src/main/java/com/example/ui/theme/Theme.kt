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
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val LocalReducedMotion = staticCompositionLocalOf { false }

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8A3D), onPrimary = Color(0xFF2B1000),
    primaryContainer = Color(0xFF45251D), onPrimaryContainer = Color(0xFFFFD8BC),
    secondary = Color(0xFF3DE0FF), onSecondary = Color(0xFF002F39),
    secondaryContainer = Color(0xFF133C4A), onSecondaryContainer = Color(0xFFC2F5FF),
    tertiary = Color(0xFFFF7EB6), onTertiary = Color(0xFF4D0026),
    tertiaryContainer = Color(0xFF4A1936), onTertiaryContainer = Color(0xFFFFD8E8),
    background = Color(0xFF0F0D24), onBackground = Color(0xFFF1EEFF),
    surface = Color(0xFF181538), onSurface = Color(0xFFF1EEFF),
    surfaceVariant = Color(0xFF211D4A), onSurfaceVariant = Color(0xFFBDB6E6),
    surfaceContainerLowest = Color(0xFF0B091E), surfaceContainerLow = Color(0xFF181538),
    surfaceContainer = Color(0xFF211D4A), surfaceContainerHigh = Color(0xFF292353),
    surfaceContainerHighest = Color(0xFF332C61),
    outline = Color(0xFF9D94C7), outlineVariant = Color(0xFF3A3470),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005), errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC2410C), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDFCE), onPrimaryContainer = Color(0xFF602000),
    secondary = Color(0xFF0E7490), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F3F8), onSecondaryContainer = Color(0xFF003A48),
    tertiary = Color(0xFFBE185D), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDFEB), onTertiaryContainer = Color(0xFF66082F),
    background = Color(0xFFFFF8F0), onBackground = Color(0xFF1F1A3D),
    surface = Color.White, onSurface = Color(0xFF1F1A3D),
    surfaceVariant = Color(0xFFFFEFE0), onSurfaceVariant = Color(0xFF5A5380),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFF8F0),
    surfaceContainer = Color(0xFFFFEFE0), surfaceContainerHigh = Color(0xFFFFE8D7),
    surfaceContainerHighest = Color(0xFFF8E0D0),
    outline = Color(0xFF80759A), outlineVariant = Color(0xFFE8D5C0),
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
    CompositionLocalProvider(LocalReducedMotion provides reducedMotion, LocalJourneyColors provides if (dark) JourneyColors.dark else JourneyColors.light) {
        MaterialTheme(colorScheme = if (dark) DarkColorScheme else LightColorScheme, typography = Typography,
            shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(20.dp)), content = content)
    }
}

