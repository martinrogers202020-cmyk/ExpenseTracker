package com.example.expensetracker.data.prefs

import com.example.expensetracker.data.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProManager(
    private val repo: SettingsRepository
) {
    // Pro comes from AppearancePrefs.proEnabled inside SettingsRepository.appearance
    val isProFlow: Flow<Boolean> = repo.appearance.map { it.proEnabled }

    suspend fun setPro(enabled: Boolean) {
        repo.setProEnabled(enabled)
    }
}
