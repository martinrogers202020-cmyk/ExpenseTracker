// FILE: app/src/main/java/com/example/expensetracker/data/recurring/RecurringEngine.kt
package com.example.expensetracker.data.recurring

import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecurringEngine(
    private val db: AppDatabase
) {
    suspend fun runIfDue(todayEpochDay: Long) = withContext(Dispatchers.IO) {
        val due = db.recurringTransactionDao().getDue(todayEpochDay = todayEpochDay)
        if (due.isEmpty()) return@withContext

        val txDao = db.transactionDao()
        val recDao = db.recurringTransactionDao()

        due.forEach { r ->
            val runDay = r.nextDueEpochDay

            val tx = TransactionEntity(
                id = 0L,
                type = r.type.toTransactionTypeOrDefault(),
                amountCents = r.amountCents,
                categoryId = r.categoryId ?: 0L,
                note = r.title,
                epochDay = runDay,
                recurringId = r.id,
                recurringRunDateEpoch = runDay
            )
            txDao.insert(tx)

            var next = r.nextDueEpochDay
            val step = r.frequencyDays.coerceAtLeast(1)
            while (next <= todayEpochDay) next += step.toLong()

            recDao.upsert(
                r.copy(
                    nextDueEpochDay = next
                )
            )
        }
    }
}

private fun String.toTransactionTypeOrDefault(): TransactionType {
    return try {
        TransactionType.valueOf(this)
    } catch (_: Throwable) {
        TransactionType.EXPENSE
    }
}
