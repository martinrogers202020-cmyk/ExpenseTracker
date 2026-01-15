package com.example.expensetracker.data.repo

import android.content.Context
import com.example.expensetracker.R
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

    suspend fun syncLocalizedDefaultCategoryNames(context: Context) {
        data class DefaultCategoryLocalization(
            val emoji: String,
            val knownNames: List<String>,
            val nameResId: Int
        )

        val mappings = listOf(
            DefaultCategoryLocalization(
                emoji = "🧾",
                knownNames = listOf("Bills", "Faturalar"),
                nameResId = R.string.category_default_bills
            ),
            DefaultCategoryLocalization(
                emoji = "☕",
                knownNames = listOf("Coffee", "Kahve"),
                nameResId = R.string.category_default_coffee
            ),
            DefaultCategoryLocalization(
                emoji = "🍔",
                knownNames = listOf("Eating Out", "Dışarıda yeme"),
                nameResId = R.string.category_default_eating_out
            ),
            DefaultCategoryLocalization(
                emoji = "🛒",
                knownNames = listOf("Groceries", "Market"),
                nameResId = R.string.category_default_groceries
            ),
            DefaultCategoryLocalization(
                emoji = "💙",
                knownNames = listOf("Health", "Sağlık"),
                nameResId = R.string.category_default_health
            ),
            DefaultCategoryLocalization(
                emoji = "🏠",
                knownNames = listOf("Rent", "Kira"),
                nameResId = R.string.category_default_rent
            )
        )

        mappings.forEach { mapping ->
            val newName = context.getString(mapping.nameResId)
            dao.renameDefaultCategoryByEmoji(mapping.emoji, mapping.knownNames, newName)
        }
    }
}
