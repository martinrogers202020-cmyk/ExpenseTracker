package com.example.expensetracker.ui.state

import androidx.compose.runtime.Immutable

@Immutable
data class BudgetsUiState(
    val monthLabel: String = "",
    val year: Int = 0,
    val month: Int = 0,
    val categories: List<CategoryOptionUi> = emptyList(),
    val items: List<BudgetItemUi> = emptyList()
)

@Immutable
data class CategoryOptionUi(
    val id: Long,
    val label: String
)
