package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.TransactionRepository
import com.example.expensetracker.ui.state.HomeCategoryUi
import com.example.expensetracker.ui.state.HomeUiState
import com.example.expensetracker.ui.state.MoneySummary
import com.example.expensetracker.ui.state.TransactionItemUi
import com.example.expensetracker.ui.state.TxTypeFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

class HomeViewModel(
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val searchQuery = MutableStateFlow("")
    private val typeFilter = MutableStateFlow(TxTypeFilter.ALL)
    private val categoryFilterId = MutableStateFlow<Long?>(null)

    private val filtersFlow: StateFlow<Filters> =
        combine(selectedMonth, searchQuery, typeFilter, categoryFilterId) { month, query, typeF, catId ->
            Filters(month = month, query = query, typeFilter = typeF, categoryId = catId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Filters()
        )

    val uiState: StateFlow<HomeUiState> =
        combine(
            filtersFlow,
            transactionRepo.observeAll(),
            categoryRepo.observeCategories()
        ) { filters, transactions, categories ->

            val categoryMap = categories.associateBy { it.id }

            val categoryUi: List<HomeCategoryUi> = categories.map { c ->
                HomeCategoryUi(
                    id = c.id,
                    label = "${c.emoji} ${c.name}"
                )
            }

            // 1) Month filter (base for everything)
            val monthTxs = transactions.filter { tx ->
                val date = LocalDate.ofEpochDay(tx.epochDay)
                YearMonth.from(date) == filters.month
            }

            // ✅ Summary based on ALL monthTxs, using ABS cents
            var monthIncome = 0L
            var monthExpense = 0L
            for (tx in monthTxs) {
                val amt = abs(tx.amountCents)
                when (tx.type) {
                    TransactionType.INCOME -> monthIncome += amt
                    TransactionType.EXPENSE -> monthExpense += amt
                }
            }

            // 2) Apply list filters ONLY to the list
            val typeFiltered = monthTxs.filter { tx ->
                when (filters.typeFilter) {
                    TxTypeFilter.ALL -> true
                    TxTypeFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
                    TxTypeFilter.INCOME -> tx.type == TransactionType.INCOME
                }
            }

            val catFiltered = typeFiltered.filter { tx ->
                filters.categoryId == null || tx.categoryId == filters.categoryId
            }

            val q = filters.query.trim().lowercase()
            val searchFiltered = if (q.isBlank()) {
                catFiltered
            } else {
                catFiltered.filter { tx ->
                    val catName = categoryMap[tx.categoryId]?.name?.lowercase().orEmpty()
                    val note = tx.note.orEmpty().lowercase()
                    catName.contains(q) || note.contains(q)
                }
            }

            // 3) Map to UI items (sorted)
            val items: List<TransactionItemUi> = searchFiltered
                .sortedWith(
                    compareByDescending<TransactionEntity> { it.epochDay }
                        .thenByDescending { it.id }
                )
                .map { tx ->
                    val cat = categoryMap[tx.categoryId]
                    val categoryName = cat?.name ?: "Unknown"
                    val emoji = cat?.emoji ?: "❓"
                    val displayTitle = "$emoji $categoryName"

                    TransactionItemUi(
                        id = tx.id,
                        title = displayTitle,
                        categoryName = categoryName,        // ✅ NEW field for exports
                        note = tx.note.orEmpty(),
                        amountCents = abs(tx.amountCents),  // ✅ keep UI consistent with exports
                        type = tx.type,
                        epochDay = tx.epochDay
                    )
                }

            HomeUiState(
                month = filters.month,
                summary = MoneySummary(
                    incomeCents = monthIncome,
                    expenseCents = monthExpense
                ),
                transactions = items,
                isLoading = false,
                searchQuery = filters.query,
                typeFilter = filters.typeFilter,
                categoryFilterId = filters.categoryId,
                categories = categoryUi
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true)
        )

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonth.update { it.plusMonths(1) }
    }

    fun setSearchQuery(value: String) {
        searchQuery.value = value
    }

    fun setTypeFilter(value: TxTypeFilter) {
        typeFilter.value = value
    }

    fun setCategoryFilter(id: Long?) {
        categoryFilterId.value = id
    }

    fun clearFilters() {
        searchQuery.value = ""
        typeFilter.value = TxTypeFilter.ALL
        categoryFilterId.value = null
    }

    private data class Filters(
        val month: YearMonth = YearMonth.now(),
        val query: String = "",
        val typeFilter: TxTypeFilter = TxTypeFilter.ALL,
        val categoryId: Long? = null
    )
}
