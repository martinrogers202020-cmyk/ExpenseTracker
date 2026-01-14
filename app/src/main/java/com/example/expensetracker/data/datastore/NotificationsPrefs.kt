package com.example.expensetracker.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationsDataStore by preferencesDataStore(
    name = "notifications_prefs"
)

class NotificationsPrefs(
    private val context: Context
) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val HOUR = intPreferencesKey("hour")
        val MINUTE = intPreferencesKey("minute")
        val DAILY_REMINDER = booleanPreferencesKey("daily_reminder")
        val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
    }

    /** Read preferences as a Flow */
    val flow: Flow<NotificationPrefs> =
        context.notificationsDataStore.data.map { prefs ->
            NotificationPrefs(
                enabled = prefs[Keys.ENABLED] ?: true,
                hour = prefs[Keys.HOUR] ?: 9,
                minute = prefs[Keys.MINUTE] ?: 0,
                dailyReminder = prefs[Keys.DAILY_REMINDER] ?: true,
                budgetAlerts = prefs[Keys.BUDGET_ALERTS] ?: false
            )
        }

    /** Write methods */

    suspend fun setEnabled(value: Boolean) {
        context.notificationsDataStore.edit {
            it[Keys.ENABLED] = value
        }
    }

    suspend fun setTime(hour: Int, minute: Int) {
        context.notificationsDataStore.edit {
            it[Keys.HOUR] = hour
            it[Keys.MINUTE] = minute
        }
    }

    suspend fun setDailyReminder(value: Boolean) {
        context.notificationsDataStore.edit {
            it[Keys.DAILY_REMINDER] = value
        }
    }

    suspend fun setBudgetAlerts(value: Boolean) {
        context.notificationsDataStore.edit {
            it[Keys.BUDGET_ALERTS] = value
        }
    }
}
