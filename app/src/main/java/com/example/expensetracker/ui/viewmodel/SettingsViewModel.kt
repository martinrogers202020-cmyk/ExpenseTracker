package com.example.expensetracker.ui.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.datastore.AccentChoice
import com.example.expensetracker.data.datastore.AppearancePrefs
import com.example.expensetracker.data.datastore.LanguageTags
import com.example.expensetracker.data.datastore.ThemeMode
import com.example.expensetracker.data.repo.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            repo.languageTag
                .distinctUntilChanged()
                .collect { tag -> applyAppLocale(tag) }
        }
    }

    val appearance: StateFlow<AppearancePrefs> =
        repo.appearance.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppearancePrefs(
                themeMode = ThemeMode.SYSTEM,
                dynamicColor = true,
                accentChoice = AccentChoice.PURPLE,
                fontScale = 1.0f,
                compactSpacing = false,
                proEnabled = false,
                languageTag = LanguageTags.DEFAULT
            )
        )

    val currentLanguageTag: StateFlow<String> =
        repo.appearance
            .map { it.languageTag }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AppearancePrefs().languageTag
            )

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { repo.setThemeMode(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { repo.setDynamicColor(value) }
    fun setAccentChoice(value: AccentChoice) = viewModelScope.launch { repo.setAccentChoice(value) }
    fun setFontScale(value: Float) = viewModelScope.launch { repo.setFontScale(value) }
    fun setCompactSpacing(value: Boolean) = viewModelScope.launch { repo.setCompactSpacing(value) }
    fun setProEnabled(value: Boolean) = viewModelScope.launch { repo.setProEnabled(value) }
    fun setLanguageTag(value: String) = viewModelScope.launch { repo.setLanguageTag(value) }

    fun updateLanguage(tag: String): Boolean {
        val newLocales = if (tag == LanguageTags.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val newTags = newLocales.toLanguageTags()
        val changed = currentTags != newTags

        viewModelScope.launch { repo.setLanguageTag(tag) }

        if (changed) {
            AppCompatDelegate.setApplicationLocales(newLocales)
        }
        return changed
    }

    private fun applyAppLocale(tag: String) {
        val newLocales = if (tag == LanguageTags.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(tag)
        }
        val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val newTags = newLocales.toLanguageTags()
        if (currentTags != newTags) {
            AppCompatDelegate.setApplicationLocales(newLocales)
        }
    }
}
