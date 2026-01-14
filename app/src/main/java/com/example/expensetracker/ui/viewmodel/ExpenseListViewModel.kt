package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ExpenseRowUi(
    val id: Long,
    val categoryLabel: String,
    val note: String,
    val amountCents: Long,
    val epochDay: Long
)

data class ExpenseGroupUi(
    val categoryLabel: String,
    val totalCents: Long,
    val items: List<ExpenseRowUi>
)

data class ExpensesUiState(
    val groups: List<ExpenseGroupUi> = emptyList(),
    val isLoading: Boolean = true
)

class ExpenseListViewModel(
    txRepo: TransactionRepository,
    catRepo: CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<ExpensesUiState> =
        combine(
            txRepo.observeAll(),            // all transactions
            catRepo.observeCategories()     // categories
        ) { txs, cats ->
            val catMap = cats.associateBy { it.id }

            val expenses = txs.filter { it.type == TransactionType.EXPENSE }

            // map to row UI
            val rows = expenses.map { tx ->
                val cat = catMap[tx.categoryId]
                val label = if (cat != null) "${cat.emoji} ${cat.name}" else "❓ Unknown"
                ExpenseRowUi(
                    id = tx.id,
                    categoryLabel = label,
                    note = tx.note.orEmpty(),
                    amountCents = tx.amountCents,
                    epochDay = tx.epochDay
                )
            }

            // group by category label
            val groups = rows
                .groupBy { it.categoryLabel }
                .map { (catLabel, items) ->
                    val sorted = items.sortedWith(
                        compareByDescending<ExpenseRowUi> { it.epochDay }
                            .thenByDescending { it.id }
                    )
                    ExpenseGroupUi(
                        categoryLabel = catLabel,
                        totalCents = sorted.sumOf { it.amountCents },
                        items = sorted
                    )
                }
                .sortedByDescending { it.totalCents } // most spent at top

            ExpensesUiState(groups = groups, isLoading = false)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ExpensesUiState(isLoading = true)
            )
}
