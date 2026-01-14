package com.example.expensetracker.data.db

import com.example.expensetracker.data.model.CategoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SeedRunner {

    fun run(db: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            val dao = db.categoryDao()

            // If you already have categories, do nothing
            if (dao.getAllOnce().isNotEmpty()) return@launch

            val defaults = listOf(
                CategoryEntity(id = 0L, name = "Bills", emoji = "🧾", isDefault = true),
                CategoryEntity(id = 0L, name = "Coffee", emoji = "☕", isDefault = true),
                CategoryEntity(id = 0L, name = "Eating Out", emoji = "🍔", isDefault = true),
                CategoryEntity(id = 0L, name = "Groceries", emoji = "🛒", isDefault = true),
                CategoryEntity(id = 0L, name = "Health", emoji = "💙", isDefault = true),
                CategoryEntity(id = 0L, name = "Rent", emoji = "🏠", isDefault = true),
            )

            defaults.forEach { dao.upsert(it) }
        }
    }
}
