package com.example.expensetracker.data.repo

import com.example.expensetracker.data.db.CategoryDao
import com.example.expensetracker.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val dao: CategoryDao
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun getAllOnce(): List<CategoryEntity> = dao.getAllOnce()

    suspend fun addCategory(name: String, emoji: String, isDefault: Boolean = false) {
        dao.upsert(
            CategoryEntity(
                id = 0L,
                name = name,
                emoji = emoji,
                isDefault = isDefault
            )
        )
    }

    suspend fun updateCategory(id: Long, name: String, emoji: String, isDefault: Boolean) {
        dao.upsert(
            CategoryEntity(
                id = id,
                name = name,
                emoji = emoji,
                isDefault = isDefault
            )
        )
    }

    suspend fun deleteCategory(id: Long) {
        dao.deleteById(id)
    }

    // ✅ inside the class (so repo.seedDefaultCategoriesIfEmpty() exists)
    suspend fun seedDefaultCategoriesIfEmpty(defaults: List<Pair<String, String>>) {
        val current = dao.getAllOnce()
        if (current.isNotEmpty()) return

        defaults.forEach { (name, emoji) ->
            addCategory(name, emoji, true)
        }
    }
}
