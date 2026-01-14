// FILE: app/src/main/java/com/example/expensetracker/data/model/RecurringTransactionEntity.kt
package com.example.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    val title: String,
    val amountCents: Long,

    // store enum as String (simple + stable)
    val type: String, // "INCOME" / "EXPENSE"

    val categoryId: Long? = null,
    val note: String? = null,

    // recurring config
    val frequencyDays: Int,          // e.g., 1 daily, 7 weekly, 30 monthly-ish
    val startEpochDay: Long,

    // scheduling state
    val nextDueEpochDay: Long,       // REQUIRED by your DAO error
    val lastReminderEpochDay: Long? = null,
    val remindDailyIfOverdue: Boolean = false,

    val isActive: Boolean = true,
    val createdAtEpochDay: Long
)
