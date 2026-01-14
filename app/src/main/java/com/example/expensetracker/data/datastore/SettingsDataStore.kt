package com.example.expensetracker.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val THEME_MODE = intPreferencesKey("theme_mode")          // 0=SYSTEM,1=LIGHT,2=DARK
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val ACCENT = intPreferencesKey("accent")                 // 0..3
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val COMPACT_SPACING = booleanPreferencesKey("compact_spacing")
    val PRO_ENABLED = booleanPreferencesKey("pro_enabled")
}
