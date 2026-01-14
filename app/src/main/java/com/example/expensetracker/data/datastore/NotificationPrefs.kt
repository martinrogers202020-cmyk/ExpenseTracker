package com.example.expensetracker.data.datastore

data class NotificationPrefs(
    val enabled: Boolean = true,
    val hour: Int = 9,
    val minute: Int = 0,
    val dailyReminder: Boolean = true,
    val budgetAlerts: Boolean = false
)
