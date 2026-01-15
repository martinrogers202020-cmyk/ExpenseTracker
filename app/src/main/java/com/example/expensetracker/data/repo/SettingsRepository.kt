package com.example.expensetracker.data.repo

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
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

    fun applyLanguageIfNeeded(value: String): Boolean {
        val normalized = normalizeLanguageTag(value)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            val newLocales = LocaleList.forLanguageTags(normalized)
            val currentLocales = localeManager.applicationLocales
            if (currentLocales.toLanguageTags() != newLocales.toLanguageTags()) {
                if (LOG_LANGUAGE_CHANGES) {
                    Log.d(TAG, "Applying application locales: $normalized")
                }
                localeManager.applicationLocales = newLocales
                true
            } else {
                if (LOG_LANGUAGE_CHANGES) {
                    Log.d(TAG, "Application locales already set: $normalized")
                }
                false
            }
        } else {
            val newLocales = LocaleListCompat.forLanguageTags(normalized)
            val currentLocales = AppCompatDelegate.getApplicationLocales()
            if (currentLocales.toLanguageTags() != newLocales.toLanguageTags()) {
                if (LOG_LANGUAGE_CHANGES) {
                    Log.d(TAG, "Applying appcompat locales: $normalized")
                }
                AppCompatDelegate.setApplicationLocales(newLocales)
                true
            } else {
                if (LOG_LANGUAGE_CHANGES) {
                    Log.d(TAG, "Appcompat locales already set: $normalized")
                }
                false
            }
        }
    }
}
