package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.model.BudgetEntity
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.data.repo.BudgetRepository
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.TransactionRepository
import com.example.expensetracker.ui.state.BudgetItemUi
import com.example.expensetracker.ui.state.BudgetsUiState
import com.example.expensetracker.ui.state.CategoryOptionUi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class BudgetsViewModel(
    private val budgetRepo: BudgetRepository,
    private val txRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    val yearMonth: StateFlow<YearMonth> = _yearMonth.asStateFlow()

    private val categoriesFlow = categoryRepo.observeCategories()

    private val budgetsFlow = _yearMonth.flatMapLatest { ym ->
        budgetRepo.observeForMonth(ym.year, ym.monthValue)
    }

    private val txInMonthFlow = _yearMonth.flatMapLatest { ym ->
        val start = LocalDate.of(ym.year, ym.monthValue, 1).toEpochDay()
        val endDay = LocalDate.of(ym.year, ym.monthValue, ym.lengthOfMonth()).toEpochDay()
        txRepo.observeBetweenDays(start, endDay)
    }

    val uiState: StateFlow<BudgetsUiState> =
        combine(categoriesFlow, budgetsFlow, txInMonthFlow, _yearMonth) { categories, budgets, txs, ym ->
            val spentByCategory: Map<Long, Long> = txs
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .mapValues { (_, list) -> list.sumOf { it.amountCents } }

            val items = budgets.mapNotNull { b ->
                val cat = categories.firstOrNull { it.id == b.categoryId } ?: return@mapNotNull null
                BudgetItemUi(
                    budgetId = b.id,
                    categoryId = cat.id,
                    categoryName = cat.name,
                    categoryEmoji = cat.emoji,
                    limitCents = b.limitCents,
                    spentCents = spentByCategory[cat.id] ?: 0L
                )
            }

            BudgetsUiState(
                monthLabel = ym.atDay(1).format(monthFormatter),
                year = ym.year,
                month = ym.monthValue,
                categories = categories.map { CategoryOptionUi(it.id, "${it.emoji}  ${it.name}") },
                items = items
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    fun previousMonth() {
        _yearMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        _yearMonth.update { it.plusMonths(1) }
    }

    fun upsertBudget(categoryId: Long, limitCents: Long) {
        val ym = _yearMonth.value
        viewModelScope.launch {
            budgetRepo.upsert(
                BudgetEntity(
                    categoryId = categoryId,
                    limitCents = limitCents,
                    month = ym.monthValue,
                    year = ym.year
                )
            )
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            budgetRepo.deleteById(budgetId)
        }
    }
}
