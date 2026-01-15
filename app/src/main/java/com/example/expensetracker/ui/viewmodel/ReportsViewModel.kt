package com.example.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.R
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.TransactionRepository
import com.example.expensetracker.ui.state.CategorySpendUi
import com.example.expensetracker.ui.state.ReportsUiState
import com.example.expensetracker.ui.state.TransactionItemUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import kotlin.math.abs

class ReportsViewModel(
    private val context: Context,
    private val txRepo: TransactionRepository,
    private val catRepo: CategoryRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<ReportsUiState> =
        selectedMonth
            .flatMapLatest { month ->
                val startDay = month.atDay(1).toEpochDay()
                val endDay = month.atEndOfMonth().toEpochDay()

                combine(
                    txRepo.observeBetweenDays(startDay, endDay),
                    catRepo.observeCategories()
                ) { txs, cats ->

                    val catMap = cats.associateBy { it.id }

                    val txItems: List<TransactionItemUi> = txs
                        .sortedWith(
                            compareByDescending<TransactionEntity> { it.epochDay }
                                .thenByDescending { it.id }
                        )
                        .map { tx ->
                            val cat = catMap[tx.categoryId]
                            val plainName = when {
                                cat == null -> context.getString(R.string.categories_unknown_label)
                                cat.name.isNotBlank() -> cat.name
                                else -> context.getString(R.string.categories_default_label)
                            }

                            TransactionItemUi(
                                id = tx.id,
                                title = plainName,              // ✅ keep old screens compiling
                                categoryName = plainName,       // ✅ export column
                                note = tx.note.orEmpty(),
                                amountCents = abs(tx.amountCents),
                                type = tx.type,
                                epochDay = tx.epochDay
                            )
                        }

                    var income = 0L
                    var expense = 0L
                    val expenseByCat = mutableMapOf<Long, Long>()

                    for (tx in txs) {
                        val amt = abs(tx.amountCents)
                        when (tx.type) {
                            TransactionType.INCOME -> income += amt
                            TransactionType.EXPENSE -> {
                                expense += amt
                                expenseByCat[tx.categoryId] =
                                    (expenseByCat[tx.categoryId] ?: 0L) + amt
                            }
                        }
                    }

                    val topCats: List<CategorySpendUi> =
                        expenseByCat.entries
                            .sortedByDescending { it.value }
                            .take(8)
                            .map { entry ->
                                val cat = catMap[entry.key]
                                val label = if (cat != null) {
                                    "${cat.emoji} ${cat.name}"
                                } else {
                                    context.getString(R.string.categories_unknown_with_icon)
                                }
                                val fraction = if (expense == 0L) 0f else entry.value.toFloat() / expense.toFloat()

                                CategorySpendUi(
                                    categoryId = entry.key,
                                    label = label,
                                    amountCents = entry.value,
                                    fraction = fraction
                                )
                            }

                    ReportsUiState(
                        month = month,
                        incomeCents = income,
                        expenseCents = expense,
                        balanceCents = income - expense,
                        topExpenseCategories = topCats,
                        transactions = txItems,
                        isLoading = false
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReportsUiState(isLoading = true)
            )

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonth.update { it.plusMonths(1) }
    }
}
