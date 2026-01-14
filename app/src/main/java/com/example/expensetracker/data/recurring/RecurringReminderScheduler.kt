package com.example.expensetracker.data.recurring

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object RecurringReminderScheduler {
    private const val UNIQUE_NAME = "recurring_reminder_daily"

    fun ensureScheduled(context: Context) {
        val initialDelay = computeInitialDelayToNextHour(9) // ~9 AM local time

        val req = PeriodicWorkRequestBuilder<RecurringReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    private fun computeInitialDelayToNextHour(targetHour: Int): Duration {
        val now = LocalDateTime.now()
        val next = now.withHour(targetHour).withMinute(0).withSecond(0).withNano(0)
        val scheduled = if (now.isBefore(next)) next else next.plusDays(1)
        return Duration.between(now, scheduled)
    }
}
