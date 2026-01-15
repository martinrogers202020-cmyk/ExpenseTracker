package com.example.expensetracker.data.repo

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.example.expensetracker.data.datastore.AccentChoice
import com.example.expensetracker.data.datastore.AppearancePrefs
import com.example.expensetracker.data.datastore.SettingsKeys
import com.example.expensetracker.data.datastore.ThemeMode
import com.example.expensetracker.data.datastore.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {
    private companion object {
        const val LOG_LANGUAGE_CHANGES = false
        const val TAG = "SettingsRepository"
        const val DEFAULT_LANGUAGE = "en"
    }

    private fun normalizeLanguageTag(value: String): String =
        value.ifBlank { DEFAULT_LANGUAGE }

    val appearance: Flow<AppearancePrefs> =
        context.settingsDataStore.data.map { prefs ->
            val themeMode = when (prefs[SettingsKeys.THEME_MODE] ?: 0) {
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }

            val accentChoice = when (prefs[SettingsKeys.ACCENT] ?: 0) {
                1 -> AccentChoice.BLUE
                2 -> AccentChoice.PINK
                3 -> AccentChoice.GREEN
                else -> AccentChoice.PURPLE
            }

            AppearancePrefs(
                themeMode = themeMode,
                dynamicColor = prefs[SettingsKeys.DYNAMIC_COLOR] ?: true,
                accentChoice = accentChoice,
                fontScale = prefs[SettingsKeys.FONT_SCALE] ?: 1.0f,
                compactSpacing = prefs[SettingsKeys.COMPACT_SPACING] ?: false,
                proEnabled = prefs[SettingsKeys.PRO_ENABLED] ?: false,
                languageTag = prefs[SettingsKeys.LANGUAGE_TAG] ?: DEFAULT_LANGUAGE
            )
        }

    suspend fun setThemeMode(value: ThemeMode) {
        context.settingsDataStore.edit { it[SettingsKeys.THEME_MODE] = value.ordinal }
    }

    suspend fun setDynamicColor(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.DYNAMIC_COLOR] = value }
    }

    suspend fun setAccentChoice(value: AccentChoice) {
        context.settingsDataStore.edit { it[SettingsKeys.ACCENT] = value.ordinal }
    }

    suspend fun setFontScale(value: Float) {
        context.settingsDataStore.edit { it[SettingsKeys.FONT_SCALE] = value }
    }

    suspend fun setCompactSpacing(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.COMPACT_SPACING] = value }
    }

    suspend fun setProEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.PRO_ENABLED] = value }
    }
    suspend fun enablePro() = setProEnabled(true)
    suspend fun disablePro() = setProEnabled(false)

    suspend fun setLanguageTag(value: String) {
        val normalized = normalizeLanguageTag(value)
        val current = context.settingsDataStore.data.first()[SettingsKeys.LANGUAGE_TAG] ?: DEFAULT_LANGUAGE
        if (current == normalized) {
            if (LOG_LANGUAGE_CHANGES) {
                Log.d(TAG, "Language preference unchanged: $normalized")
            }
            return
        }
        if (LOG_LANGUAGE_CHANGES) {
            Log.d(TAG, "Persisting language preference: $normalized")
        }
        context.settingsDataStore.edit { it[SettingsKeys.LANGUAGE_TAG] = normalized }
    }
}
