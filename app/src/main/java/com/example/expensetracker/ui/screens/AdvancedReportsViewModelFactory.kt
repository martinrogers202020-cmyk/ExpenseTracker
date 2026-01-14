// FILE: app/src/main/java/com/example/expensetracker/ui/viewmodel/AdvancedReportsViewModelFactory.kt
package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.data.db.AppDatabase

class AdvancedReportsViewModelFactory(
    private val db: AppDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdvancedReportsViewModel::class.java)) {
            return AdvancedReportsViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
