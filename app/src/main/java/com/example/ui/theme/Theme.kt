package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CinematicRed,
    onPrimary = Color.White,
    secondary = ObsidianCard,
    onSecondary = TextPrimary,
    background = ObsidianDark,
    onBackground = TextPrimary,
    surface = ObsidianCard,
    onSurface = TextPrimary,
    tertiary = AccentAmber,
    surfaceVariant = Color(0xFF1F2235),
    onSurfaceVariant = TextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = CinematicRed,
    onPrimary = Color.White,
    secondary = ObsidianCard,
    onSecondary = TextPrimary,
    background = ObsidianDark,
    onBackground = TextPrimary,
    surface = ObsidianCard,
    onSurface = TextPrimary,
    tertiary = AccentAmber,
    surfaceVariant = Color(0xFF1F2235),
    onSurfaceVariant = TextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce our cinematic brand styling
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
