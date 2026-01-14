package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.datastore.NotificationsPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val prefs: NotificationsPrefs
) : ViewModel() {

    val state = prefs.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = com.example.expensetracker.data.datastore.NotificationPrefs()
    )

    fun setEnabled(value: Boolean) = viewModelScope.launch { prefs.setEnabled(value) }

    fun setTime(hour: Int, minute: Int) = viewModelScope.launch { prefs.setTime(hour, minute) }

    fun setDailyReminder(value: Boolean) = viewModelScope.launch { prefs.setDailyReminder(value) }

    fun setBudgetAlerts(value: Boolean) = viewModelScope.launch { prefs.setBudgetAlerts(value) }
}
