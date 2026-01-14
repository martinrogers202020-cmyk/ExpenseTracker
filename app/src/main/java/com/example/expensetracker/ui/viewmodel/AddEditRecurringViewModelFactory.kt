package com.example.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.data.db.DatabaseProvider
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.RecurringTransactionRepository

class AddEditRecurringViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = DatabaseProvider.get(context)
        return AddEditRecurringViewModel(
            RecurringTransactionRepository(db.recurringTransactionDao()),
            CategoryRepository(db.categoryDao())
        ) as T
    }
}
