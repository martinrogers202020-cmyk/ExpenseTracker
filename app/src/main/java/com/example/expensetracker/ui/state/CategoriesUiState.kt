package com.example.expensetracker.ui.state

data class CategoryRowUi(
    val id: Long,
    val name: String,
    val emoji: String,
    val isDefault: Boolean
)

data class CategoriesUiState(
    val categories: List<CategoryRowUi> = emptyList(),
    val error: String? = null
)
