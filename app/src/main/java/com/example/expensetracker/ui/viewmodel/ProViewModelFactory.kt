package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.data.prefs.ProManager

class ProViewModelFactory(
    private val proManager: ProManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProViewModel::class.java)) {
            return ProViewModel(proManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
