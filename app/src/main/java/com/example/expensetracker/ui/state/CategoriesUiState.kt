package com.example.expensetracker.ui.state

import androidx.compose.runtime.Immutable

@Immutable
data class CategoryRowUi(
    val id: Long,
    val name: String,
    val emoji: String,
    val isDefault: Boolean
)

@Immutable
data class CategoriesUiState(
    val categories: List<CategoryRowUi> = emptyList(),
    val error: String? = null
)
