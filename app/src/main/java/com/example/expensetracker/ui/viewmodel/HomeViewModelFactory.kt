package com.example.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.data.db.DatabaseProvider
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.TransactionRepository

class HomeViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = DatabaseProvider.get(context.applicationContext)
        val txRepo = TransactionRepository(db.transactionDao())
        val catRepo = CategoryRepository(db.categoryDao())
        return HomeViewModel(txRepo, catRepo) as T
    }
}
