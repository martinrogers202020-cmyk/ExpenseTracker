package com.example.expensetracker.ui.state

import androidx.compose.runtime.Immutable
import com.example.expensetracker.data.model.TransactionType
import java.time.LocalDate

@Immutable
data class AddEditUiState(
    val isEdit: Boolean = false,
    val transactionId: Long? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val categoryId: Long? = null,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val categories: List<CategoryUi> = emptyList(),
    val error: String? = null
)

@Immutable
data class CategoryUi(
    val id: Long,
    val label: String
)
