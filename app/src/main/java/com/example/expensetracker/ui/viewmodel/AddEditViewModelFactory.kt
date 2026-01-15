package com.example.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.data.db.DatabaseProvider
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.TransactionRepository

class AddTransactionViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext
        val db = DatabaseProvider.get(appContext)
        val categoryRepo = CategoryRepository(db.categoryDao())
        val txRepo = TransactionRepository(db.transactionDao())
        return AddTransactionViewModel(appContext, categoryRepo, txRepo) as T
    }
}
