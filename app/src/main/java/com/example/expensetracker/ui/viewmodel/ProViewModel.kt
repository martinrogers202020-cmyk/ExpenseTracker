package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.prefs.ProManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProViewModel(
    private val proManager: ProManager
) : ViewModel() {

    val isPro: StateFlow<Boolean> =
        proManager.isProFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun setPro(enabled: Boolean) {
        viewModelScope.launch {
            proManager.setPro(enabled)
        }
    }
}
