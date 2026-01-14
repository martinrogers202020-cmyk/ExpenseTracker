package com.example.expensetracker.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationDataStore by preferencesDataStore(name = "notification_prefs")

class NotificationDataStore(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val HOUR = intPreferencesKey("hour")
        val MINUTE = intPreferencesKey("minute")
        val DAILY = booleanPreferencesKey("daily_reminder")
        val BUDGET = booleanPreferencesKey("budget_alerts")
    }

    val prefs: Flow<NotificationPrefs> = context.notificationDataStore.data.map { p ->
        NotificationPrefs(
            enabled = p[Keys.ENABLED] ?: true,
            hour = p[Keys.HOUR] ?: 9,
            minute = p[Keys.MINUTE] ?: 0,
            dailyReminder = p[Keys.DAILY] ?: true,
            budgetAlerts = p[Keys.BUDGET] ?: false
        )
    }

    suspend fun setEnabled(value: Boolean) {
        context.notificationDataStore.edit { it[Keys.ENABLED] = value }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.notificationDataStore.edit {
            it[Keys.HOUR] = hour.coerceIn(0, 23)
            it[Keys.MINUTE] = minute.coerceIn(0, 59)
        }
    }

    suspend fun setDailyReminder(value: Boolean) {
        context.notificationDataStore.edit { it[Keys.DAILY] = value }
    }

    suspend fun setBudgetAlerts(value: Boolean) {
        context.notificationDataStore.edit { it[Keys.BUDGET] = value }
    }

    suspend fun resetDefaults() {
        context.notificationDataStore.edit {
            it[Keys.ENABLED] = true
            it[Keys.HOUR] = 9
            it[Keys.MINUTE] = 0
            it[Keys.DAILY] = true
            it[Keys.BUDGET] = false
        }
    }
}
