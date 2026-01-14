package com.example.expensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["recurringId", "recurringRunDateEpoch"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: TransactionType,
    val amountCents: Long,
    val categoryId: Long,
    val note: String,
    val epochDay: Long,
    val recurringId: Long? = null,
    val recurringRunDateEpoch: Long? = null
)
