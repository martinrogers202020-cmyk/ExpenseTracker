package com.example.expensetracker.ui.state

import androidx.compose.runtime.Immutable
import java.time.YearMonth

@Immutable
data class CategorySpendUi(
    val categoryId: Long,
    val label: String,        // "🛒 Groceries"
    val amountCents: Long,
    val fraction: Float       // 0.0..1.0 of total expenses
)

@Immutable
data class ReportsUiState(
    val month: YearMonth = YearMonth.now(),
    val incomeCents: Long = 0,
    val expenseCents: Long = 0,
    val balanceCents: Long = 0,
    val topExpenseCategories: List<CategorySpendUi> = emptyList(),
    val isLoading: Boolean = true,
    val transactions: List<TransactionItemUi> = emptyList()
)
