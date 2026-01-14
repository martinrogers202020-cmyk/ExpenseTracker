package com.example.expensetracker.ui.state

import com.example.expensetracker.data.model.TransactionType
import java.time.YearMonth

enum class TxTypeFilter { ALL, EXPENSE, INCOME }

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val summary: MoneySummary = MoneySummary(),
    val transactions: List<TransactionItemUi> = emptyList(),
    val isLoading: Boolean = true,

    // NEW (Day 8)
    val searchQuery: String = "",
    val typeFilter: TxTypeFilter = TxTypeFilter.ALL,
    val categoryFilterId: Long? = null,
    val categories: List<HomeCategoryUi> = emptyList()
)

data class HomeCategoryUi(
    val id: Long,
    val label: String
)
