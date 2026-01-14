package com.example.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.expensetracker.data.db.AppDatabase

class AddEditViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "expense_tracker.db"
        ).build()

        return AddEditViewModel(
            categoryDao = db.categoryDao(),
            transactionDao = db.transactionDao()
        ) as T
    }
}
