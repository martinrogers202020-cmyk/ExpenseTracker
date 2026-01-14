package com.example.expensetracker.data.backup

import com.example.expensetracker.data.model.BudgetEntity
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.data.model.TransactionEntity

data class BackupPayload(
    val version: Int = 1,
    val createdAtEpochMillis: Long,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>
)
