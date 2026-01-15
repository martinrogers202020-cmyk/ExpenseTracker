package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.datastore.AccentChoice
import com.example.expensetracker.data.datastore.AppearancePrefs
import com.example.expensetracker.data.datastore.ThemeMode
import com.example.expensetracker.data.repo.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

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
                languageTag = "en"
            )
        )

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { repo.setThemeMode(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { repo.setDynamicColor(value) }
    fun setAccentChoice(value: AccentChoice) = viewModelScope.launch { repo.setAccentChoice(value) }
    fun setFontScale(value: Float) = viewModelScope.launch { repo.setFontScale(value) }
    fun setCompactSpacing(value: Boolean) = viewModelScope.launch { repo.setCompactSpacing(value) }
    fun setProEnabled(value: Boolean) = viewModelScope.launch { repo.setProEnabled(value) }
    fun setLanguageTag(value: String) = viewModelScope.launch { repo.setLanguageTag(value) }
}
