package com.example.model

enum class ThemeMode(
    val title: String,
    val subtitle: String
) {
    DARK("Dark Mode", "High-contrast neon dark aesthetic for low light & gaming"),
    LIGHT("Light Mode", "High-visibility clean light theme for daytime use"),
    SYSTEM("System Default", "Follows your device system display mode")
}
