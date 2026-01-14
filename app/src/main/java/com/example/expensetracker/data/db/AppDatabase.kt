// FILE: app/src/main/java/com/example/expensetracker/data/db/AppDatabase.kt
package com.example.expensetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.expensetracker.data.model.BudgetEntity
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.data.model.MerchantRuleEntity
import com.example.expensetracker.data.model.RecurringTransactionEntity
import com.example.expensetracker.data.model.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        MerchantRuleEntity::class
    ],
    version = 2, // ✅ bump version
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun merchantRuleDao(): MerchantRuleDao
}
