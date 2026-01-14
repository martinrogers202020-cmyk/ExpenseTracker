// FILE: app/src/main/java/com/example/expensetracker/ui/viewmodel/AddEditRecurringViewModel.kt
package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.model.RecurringFrequency
import com.example.expensetracker.data.model.RecurringTransactionEntity
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.RecurringTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddEditRecurringState(
    val note: String = "",
    val amount: String = "",
    val categoryId: Long? = null,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val interval: Int = 1
)

class AddEditRecurringViewModel(
    private val recurringRepo: RecurringTransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditRecurringState())
    val state: StateFlow<AddEditRecurringState> = _state

    val categories = categoryRepo.observeCategories()

    private var loadedId: Long? = null
    private var loadedStartEpochDay: Long = LocalDate.now().toEpochDay()
    private var loadedNextDueEpochDay: Long = LocalDate.now().toEpochDay()
    private var loadedActive: Boolean = true
    private var loadedCreatedAtEpochDay: Long = LocalDate.now().toEpochDay()
    private var loadedRemindDailyIfOverdue: Boolean = false
    private var loadedLastReminderEpochDay: Long? = null

    fun updateNote(v: String) = _state.update { it.copy(note = v) }
    fun updateAmount(v: String) = _state.update { it.copy(amount = v) }
    fun updateCategory(id: Long) = _state.update { it.copy(categoryId = id) }
    fun updateFrequency(f: RecurringFrequency) = _state.update { it.copy(frequency = f) }
    fun updateInterval(i: Int) = _state.update { it.copy(interval = i.coerceAtLeast(1)) }

    fun load(id: Long) {
        viewModelScope.launch {
            val e = recurringRepo.getAllOnce().firstOrNull { it.id == id } ?: return@launch

            loadedId = e.id
            loadedStartEpochDay = e.startEpochDay
            loadedNextDueEpochDay = e.nextDueEpochDay
            loadedActive = e.isActive
            loadedCreatedAtEpochDay = e.createdAtEpochDay
            loadedRemindDailyIfOverdue = e.remindDailyIfOverdue
            loadedLastReminderEpochDay = e.lastReminderEpochDay

            val (freq, interval) = e.frequencyDays.toFrequencyAndInterval()

            _state.value = AddEditRecurringState(
                note = e.title,
                amount = (e.amountCents / 100.0).toString(),
                categoryId = e.categoryId,
                frequency = freq,
                interval = interval.coerceAtLeast(1)
            )
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            val id = loadedId ?: return@launch
            val e = recurringRepo.getAllOnce().firstOrNull { it.id == id } ?: return@launch
            recurringRepo.delete(e)
            onDone()
        }
    }

    fun save(type: TransactionType, onDone: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            val catId = s.categoryId ?: return@launch

            val amountDouble = s.amount.trim().replace(",", ".").toDoubleOrNull() ?: return@launch
            if (amountDouble <= 0.0) return@launch

            val cents = (amountDouble * 100.0).toLong()
            val today = LocalDate.now().toEpochDay()

            val frequencyDays = s.frequency.toBaseDays() * s.interval.coerceAtLeast(1)

            val entity = RecurringTransactionEntity(
                id = loadedId ?: 0L,
                title = s.note,
                type = type.name,
                amountCents = cents,
                categoryId = catId,
                note = null,

                frequencyDays = frequencyDays,
                startEpochDay = if (loadedId != null) loadedStartEpochDay else today,
                nextDueEpochDay = if (loadedId != null) loadedNextDueEpochDay else today,

                isActive = if (loadedId != null) loadedActive else true,
                remindDailyIfOverdue = loadedRemindDailyIfOverdue,
                lastReminderEpochDay = loadedLastReminderEpochDay,

                createdAtEpochDay = if (loadedId != null) loadedCreatedAtEpochDay else today
            )

            recurringRepo.upsert(entity)
            onDone()
        }
    }
}

private fun RecurringFrequency.toBaseDays(): Int = when (this) {
    RecurringFrequency.DAILY -> 1
    RecurringFrequency.MONTHLY -> 30
    RecurringFrequency.YEARLY -> 365
}

private fun Int.toFrequencyAndInterval(): Pair<RecurringFrequency, Int> {
    if (this <= 0) return RecurringFrequency.MONTHLY to 1

    return when {
        this % 365 == 0 -> RecurringFrequency.YEARLY to (this / 365).coerceAtLeast(1)
        this % 30 == 0 -> RecurringFrequency.MONTHLY to (this / 30).coerceAtLeast(1)
        else -> RecurringFrequency.DAILY to this.coerceAtLeast(1)
    }
}
