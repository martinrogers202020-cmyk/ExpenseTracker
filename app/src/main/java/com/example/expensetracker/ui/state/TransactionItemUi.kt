package com.example.expensetracker.ui.state

import androidx.compose.runtime.Immutable
import com.example.expensetracker.data.model.TransactionType

@Immutable
data class TransactionItemUi(
    val id: Long,
    val title: String,              // UI label (can include emoji)
    val note: String,
    val amountCents: Long,          // store absolute cents
    val type: TransactionType,
    val epochDay: Long,
    val categoryName: String = title // ✅ export-safe category column (defaults to title for old screens)
)
