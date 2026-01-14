// FILE: app/src/main/java/com/example/expensetracker/data/recurring/RecurringReminderWorker.kt
package com.example.expensetracker.data.recurring

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.data.db.DatabaseProvider
import java.time.LocalDate

class RecurringReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val db = DatabaseProvider.get(applicationContext)
        val today = LocalDate.now().toEpochDay()

        val dueCount = db.recurringTransactionDao().countDue(today)
        if (dueCount <= 0) return Result.success()

        // TODO: show a notification
        // For now just run engine so items get created
        RecurringEngine(db).runIfDue(today)

        return Result.success()
    }
}
