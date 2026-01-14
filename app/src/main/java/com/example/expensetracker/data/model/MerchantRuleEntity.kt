package com.example.expensetracker.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "merchant_rules",
    indices = [Index(value = ["pattern"], unique = false)]
)
data class MerchantRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val pattern: String,               // e.g. "amazon"
    val matchType: String = "CONTAINS",// CONTAINS | STARTS_WITH | REGEX
    val categoryId: Long,              // map to this category
    val priority: Int = 0,             // higher wins
    val enabled: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
