package com.example.expensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "year", "month"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long,
    /** Stored in cents, same convention as TransactionEntity.amountCents */
    val limitCents: Long,
    /** 1..12 */
    val month: Int,
    /** e.g. 2026 */
    val year: Int
)
