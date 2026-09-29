package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Raw Base Brand Colors
val RawFoxyPrimary = Color(0xFF8C52FF)
val RawFoxyPrimaryGlow = Color(0xFFB388FF)
val RawFoxyPrimaryDark = Color(0xFF6C3CE8)
val RawFoxySecondary = Color(0xFF5E60CE)
val RawFoxyAccentPink = Color(0xFFFF2A6D)
val RawFoxyAccentCyan = Color(0xFF00F5D4)
val RawFoxyRecordRed = Color(0xFFFF2A55)
val RawFoxyWarning = Color(0xFFFFBE0B)

/**
 * FoxyCustomColors holds all semantic colors for the applet,
 * adapting between Dark Mode and Light Mode for optimal accessibility.
 */
data class FoxyCustomColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val cardElevated: Color,
    val border: Color,
    val borderBright: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val primaryGlow: Color,
    val primaryDark: Color,
    val secondary: Color,
    val accentPink: Color,
    val accentCyan: Color,
    val recordRed: Color,
    val warning: Color,
    val isDark: Boolean
)

val DarkFoxyColors = FoxyCustomColors(
    background = Color(0xFF0C0A17),
    surface = Color(0xFF141026),
    surfaceVariant = Color(0xFF1D1736),
    cardElevated = Color(0xFF261F46),
    border = Color(0x338C52FF),
    borderBright = Color(0x66B388FF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB4B0C8),
    textTertiary = Color(0xFF75718E),
    primary = Color(0xFF8C52FF),
    primaryGlow = Color(0xFFB388FF),
    primaryDark = Color(0xFF6C3CE8),
    secondary = Color(0xFF5E60CE),
    accentPink = Color(0xFFFF2A6D),
    accentCyan = Color(0xFF00F5D4),
    recordRed = Color(0xFFFF2A55),
    warning = Color(0xFFFFBE0B),
    isDark = true
)

val LightFoxyColors = FoxyCustomColors(
    background = Color(0xFFF7F6FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFEBF8),
    cardElevated = Color(0xFFFFFFFF),
    border = Color(0x338C52FF),
    borderBright = Color(0x668C52FF),
    textPrimary = Color(0xFF171426),
    textSecondary = Color(0xFF4C4765),
    textTertiary = Color(0xFF7C7799),
    primary = Color(0xFF6C3CE8),
    primaryGlow = Color(0xFF5A25D0),
    primaryDark = Color(0xFF4A18B8),
    secondary = Color(0xFF4C4FA8),
    accentPink = Color(0xFFD81B60),
    accentCyan = Color(0xFF00796B),
    recordRed = Color(0xFFD32F2F),
    warning = Color(0xFFD97706),
    isDark = false
)

val LocalFoxyColors = staticCompositionLocalOf { DarkFoxyColors }

object FoxyTheme {
    val colors: FoxyCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalFoxyColors.current
}

// Dynamic Theme Color Accessors for Compose
val FoxyBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.background

val FoxySurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.surface

val FoxySurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.surfaceVariant

val FoxyCardElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.cardElevated

val FoxyBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.border

val FoxyBorderBright: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.borderBright

val FoxyTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.textPrimary

val FoxyTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.textSecondary

val FoxyTextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.textTertiary

val FoxyPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.primary

val FoxyPrimaryGlow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.primaryGlow

val FoxyPrimaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.primaryDark

val FoxySecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.secondary

val FoxyAccentPink: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.accentPink

val FoxyAccentCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.accentCyan

val FoxyRecordRed: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.recordRed

val FoxyWarning: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalFoxyColors.current.warning
