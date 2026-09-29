package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FoxyDarkColorScheme = darkColorScheme(
    primary = FoxyPrimary,
    onPrimary = Color.White,
    primaryContainer = FoxyCardElevated,
    onPrimaryContainer = FoxyPrimaryGlow,
    secondary = FoxySecondary,
    onSecondary = Color.White,
    secondaryContainer = FoxySurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = FoxyAccentCyan,
    onTertiary = Color.Black,
    error = FoxyRecordRed,
    onError = Color.White,
    background = FoxyBackground,
    onBackground = FoxyTextPrimary,
    surface = FoxySurface,
    onSurface = FoxyTextPrimary,
    surfaceVariant = FoxySurfaceVariant,
    onSurfaceVariant = FoxyTextSecondary,
    outline = FoxyBorder,
    outlineVariant = FoxyBorderBright
)

@Composable
fun FoxyScreenRecorderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Dark-first aesthetic is required for creator/recorder
    MaterialTheme(
        colorScheme = FoxyDarkColorScheme,
        typography = Typography,
        content = content
    )
}
