package com.example.expensetracker.ui.state

import com.example.expensetracker.data.model.RecurringFrequency
import com.example.expensetracker.data.model.TransactionType

data class RecurringUiItem(
    val id: Long,
    val title: String,
    val amountLabel: String,
    val frequency: RecurringFrequency,
    val nextRunLabel: String,
    val isActive: Boolean
)

data class RecurringUiState(
    val items: List<RecurringUiItem> = emptyList()
)
