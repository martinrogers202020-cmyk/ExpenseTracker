package com.example.expensetracker.data.db

import com.example.expensetracker.data.model.CategoryEntity

object SeedData {

    fun defaultCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(name = "Groceries", emoji = "🛒", isDefault = true),
        CategoryEntity(name = "Transport", emoji = "🚌", isDefault = true),
        CategoryEntity(name = "Bills", emoji = "💡", isDefault = true),
        CategoryEntity(name = "Rent", emoji = "🏠", isDefault = true),
        CategoryEntity(name = "Eating Out", emoji = "🍔", isDefault = true),
        CategoryEntity(name = "Coffee", emoji = "☕", isDefault = true),
        CategoryEntity(name = "Health", emoji = "🩺", isDefault = true),
        CategoryEntity(name = "Salary", emoji = "💼", isDefault = true),
        CategoryEntity(name = "Other", emoji = "📦", isDefault = true)
    )
}
