package com.example.expensetracker.ui.state

import androidx.compose.runtime.Immutable
import com.example.expensetracker.data.model.RecurringFrequency

@Immutable
data class RecurringUiItem(
    val id: Long,
    val title: String,
    val amountLabel: String,
    val frequency: RecurringFrequency,
    val nextRunLabel: String,
    val isActive: Boolean
)

@Immutable
data class RecurringUiState(
    val items: List<RecurringUiItem> = emptyList()
)
