package com.example.expensetracker.ui.state

import androidx.compose.runtime.Immutable

@Immutable
data class MoneySummary(
    val incomeCents: Long = 0,
    val expenseCents: Long = 0
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}
