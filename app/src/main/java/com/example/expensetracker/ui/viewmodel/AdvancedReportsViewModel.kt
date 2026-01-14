// FILE: app/src/main/java/com/example/expensetracker/ui/viewmodel/AdvancedReportsViewModel.kt
package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TrendPoint(
    val epochDay: Long,
    val incomeCents: Long,
    val expenseCents: Long,
    val netCents: Long
)

data class AdvancedReportsState(
    val startEpochDay: Long = LocalDate.now().minusDays(30).toEpochDay(),
    val endEpochDay: Long = LocalDate.now().toEpochDay(),
    val totalIncomeCents: Long = 0L,
    val totalExpenseCents: Long = 0L,
    val netCents: Long = 0L,
    val trend: List<TrendPoint> = emptyList()
)

class AdvancedReportsViewModel(
    private val db: AppDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(AdvancedReportsState())
    val state: StateFlow<AdvancedReportsState> = _state

    fun setRange(startEpochDay: Long, endEpochDay: Long) {
        _state.update { it.copy(startEpochDay = startEpochDay, endEpochDay = endEpochDay) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val s = _state.value

            val tx = db.transactionDao().getBetween(
                startEpochDay = s.startEpochDay,
                endEpochDay = s.endEpochDay
            )

            val income = tx.asSequence()
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountCents }

            val expense = tx.asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amountCents }

            val points = buildDailyTrend(
                startEpochDay = s.startEpochDay,
                endEpochDay = s.endEpochDay,
                tx = tx
            )

            _state.update {
                it.copy(
                    totalIncomeCents = income,
                    totalExpenseCents = expense,
                    netCents = income - expense,
                    trend = points
                )
            }
        }
    }

    private fun buildDailyTrend(
        startEpochDay: Long,
        endEpochDay: Long,
        tx: List<TransactionEntity>
    ): List<TrendPoint> {
        val byDay = tx.groupBy { it.epochDay }
        val out = ArrayList<TrendPoint>((endEpochDay - startEpochDay + 1).toInt().coerceAtLeast(0))

        var d = startEpochDay
        while (d <= endEpochDay) {
            val dayTx = byDay[d].orEmpty()

            val income = dayTx.asSequence()
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountCents }

            val expense = dayTx.asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amountCents }

            out.add(
                TrendPoint(
                    epochDay = d,
                    incomeCents = income,
                    expenseCents = expense,
                    netCents = income - expense
                )
            )
            d++
        }

        return out
    }
}
