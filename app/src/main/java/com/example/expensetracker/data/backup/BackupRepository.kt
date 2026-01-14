// FILE: app/src/main/java/com/example/expensetracker/data/backup/BackupRepository.kt
package com.example.expensetracker.data.backup

import com.example.expensetracker.data.backup2.toBudgetEntities
import com.example.expensetracker.data.backup2.toCategoryEntities
import com.example.expensetracker.data.backup2.toJson
import com.example.expensetracker.data.backup2.toJsonArray
import com.example.expensetracker.data.backup2.toRecurringEntities
import com.example.expensetracker.data.backup2.toTransactionEntities
import com.example.expensetracker.data.db.AppDatabase
import org.json.JSONObject

class BackupRepository(
    private val db: AppDatabase
) {
    suspend fun exportToJsonString(): String {
        val categories = db.categoryDao().getAllOnce()
        val transactions = db.transactionDao().getAllOnce()
        val budgets = db.budgetDao().getAllOnce()
        val recurring = db.recurringTransactionDao().getAllOnce()

        val root = JSONObject().apply {
            put("schemaVersion", 1)
            put("categories", categories.toJsonArray { it.toJson() })
            put("transactions", transactions.toJsonArray { it.toJson() })
            put("budgets", budgets.toJsonArray { it.toJson() })
            put("recurring", recurring.toJsonArray { it.toJson() })
        }

        return root.toString(2)
    }

    suspend fun importFromJsonString(json: String): ImportResult {
        val root = JSONObject(json)

        val categories = root.optJSONArray("categories").toCategoryEntities()
        val transactions = root.optJSONArray("transactions").toTransactionEntities()
        val budgets = root.optJSONArray("budgets").toBudgetEntities()
        val recurring = root.optJSONArray("recurring").toRecurringEntities()

        db.categoryDao().upsertAll(categories)
        db.transactionDao().insertAll(transactions)
        db.budgetDao().upsertAll(budgets)
        db.recurringTransactionDao().upsertAll(recurring)

        return ImportResult(
            categories = categories.size,
            transactions = transactions.size,
            budgets = budgets.size,
            recurring = recurring.size
        )
    }

    data class ImportResult(
        val categories: Int,
        val transactions: Int,
        val budgets: Int,
        val recurring: Int
    )
}
