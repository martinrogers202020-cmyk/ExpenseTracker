package com.example.expensetracker.ui.state

data class BudgetItemUi(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val limitCents: Long,
    val spentCents: Long
) {
    val remainingCents: Long get() = limitCents - spentCents
    val progress: Float get() =
        if (limitCents <= 0) 0f else (spentCents.toFloat() / limitCents.toFloat()).coerceIn(0f, 1f)
}
