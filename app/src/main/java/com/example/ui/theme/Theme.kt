package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.ThemeMode

private val FoxyDarkColorScheme = darkColorScheme(
    primary = DarkFoxyColors.primary,
    onPrimary = Color.White,
    primaryContainer = DarkFoxyColors.cardElevated,
    onPrimaryContainer = DarkFoxyColors.primaryGlow,
    secondary = DarkFoxyColors.secondary,
    onSecondary = Color.White,
    secondaryContainer = DarkFoxyColors.surfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = DarkFoxyColors.accentCyan,
    onTertiary = Color.Black,
    error = DarkFoxyColors.recordRed,
    onError = Color.White,
    background = DarkFoxyColors.background,
    onBackground = DarkFoxyColors.textPrimary,
    surface = DarkFoxyColors.surface,
    onSurface = DarkFoxyColors.textPrimary,
    surfaceVariant = DarkFoxyColors.surfaceVariant,
    onSurfaceVariant = DarkFoxyColors.textSecondary,
    outline = DarkFoxyColors.border,
    outlineVariant = DarkFoxyColors.borderBright
)

private val FoxyLightColorScheme = lightColorScheme(
    primary = LightFoxyColors.primary,
    onPrimary = Color.White,
    primaryContainer = LightFoxyColors.surfaceVariant,
    onPrimaryContainer = LightFoxyColors.primaryDark,
    secondary = LightFoxyColors.secondary,
    onSecondary = Color.White,
    secondaryContainer = LightFoxyColors.surfaceVariant,
    onSecondaryContainer = LightFoxyColors.textPrimary,
    tertiary = LightFoxyColors.accentCyan,
    onTertiary = Color.White,
    error = LightFoxyColors.recordRed,
    onError = Color.White,
    background = LightFoxyColors.background,
    onBackground = LightFoxyColors.textPrimary,
    surface = LightFoxyColors.surface,
    onSurface = LightFoxyColors.textPrimary,
    surfaceVariant = LightFoxyColors.surfaceVariant,
    onSurfaceVariant = LightFoxyColors.textSecondary,
    outline = LightFoxyColors.border,
    outlineVariant = LightFoxyColors.borderBright
)

@Composable
fun FoxyScreenRecorderTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val foxyColors = if (isDark) DarkFoxyColors else LightFoxyColors
    val materialScheme = if (isDark) FoxyDarkColorScheme else FoxyLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(LocalFoxyColors provides foxyColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography,
            content = content
        )
    }
}
