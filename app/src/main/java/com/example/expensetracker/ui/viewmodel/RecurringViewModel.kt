// FILE: app/src/main/java/com/example/expensetracker/ui/viewmodel/RecurringViewModel.kt
package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.model.RecurringFrequency
import com.example.expensetracker.data.repo.RecurringTransactionRepository
import com.example.expensetracker.ui.state.RecurringUiItem
import com.example.expensetracker.ui.state.RecurringUiState
import com.example.expensetracker.util.Formatters
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class RecurringViewModel(
    private val repo: RecurringTransactionRepository
) : ViewModel() {

    val uiState = repo.observeAll()
        .map { list ->
            RecurringUiState(
                items = list.map { e ->
                    RecurringUiItem(
                        id = e.id,
                        title = e.title,
                        amountLabel = Formatters.money(e.amountCents),
                        frequency = e.frequencyDays.toRecurringFrequency(),
                        nextRunLabel = LocalDate.ofEpochDay(e.nextDueEpochDay).toString(),
                        isActive = e.isActive
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    fun toggleActive(id: Long, active: Boolean) {
        viewModelScope.launch {
            val entity = repo.getAllOnce().firstOrNull { it.id == id } ?: return@launch
            repo.upsert(entity.copy(isActive = active))
        }
    }
}

private fun Int.toRecurringFrequency(): RecurringFrequency =
    when (this) {
        1 -> RecurringFrequency.DAILY
        30 -> RecurringFrequency.MONTHLY
        365 -> RecurringFrequency.YEARLY
        else -> RecurringFrequency.MONTHLY
    }
