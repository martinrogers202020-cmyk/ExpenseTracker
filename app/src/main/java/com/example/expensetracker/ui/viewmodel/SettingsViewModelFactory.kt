package com.example.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.data.db.DatabaseProvider
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.SettingsRepository

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appContext = context.applicationContext
        val repo = SettingsRepository(appContext)
        val db = DatabaseProvider.get(appContext)
        val categoryRepository = CategoryRepository(db.categoryDao())
        return SettingsViewModel(repo, categoryRepository, appContext) as T
    }
}
