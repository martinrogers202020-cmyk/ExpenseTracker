package com.example.expensetracker.data.datastore

enum class AccentChoice { PURPLE, BLUE, PINK, GREEN }

data class AppearancePrefs(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accentChoice: AccentChoice = AccentChoice.PURPLE,
    val fontScale: Float = 1.0f,
    val compactSpacing: Boolean = false,
    val proEnabled: Boolean = false,
    val languageTag: String = "en"
)
