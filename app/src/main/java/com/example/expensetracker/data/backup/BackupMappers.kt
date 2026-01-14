// FILE: app/src/main/java/com/example/expensetracker/data/backup2/BackupMappers.kt
package com.example.expensetracker.data.backup2

import com.example.expensetracker.data.model.BudgetEntity
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.data.model.RecurringTransactionEntity
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject

internal inline fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
    val arr = JSONArray()
    for (item in this) arr.put(mapper(item))
    return arr
}

/* -------------------- EXPORT -------------------- */

internal fun CategoryEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("emoji", emoji)
}

internal fun TransactionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("type", type.name)
    put("amountCents", amountCents)
    put("categoryId", categoryId)
    put("note", note)
    put("epochDay", epochDay)
    put("recurringId", recurringId)
    put("recurringRunDateEpoch", recurringRunDateEpoch)
}

internal fun BudgetEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("categoryId", categoryId)
    put("limitCents", limitCents)
    put("month", month)
    put("year", year)
}

internal fun RecurringTransactionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("type", type) // String in your project
    put("amountCents", amountCents)
    put("categoryId", categoryId)
    put("note", note)

    put("frequencyDays", frequencyDays)
    put("startEpochDay", startEpochDay)
    put("nextDueEpochDay", nextDueEpochDay)

    put("isActive", isActive)
    put("remindDailyIfOverdue", remindDailyIfOverdue)
    put("lastReminderEpochDay", lastReminderEpochDay)

    put("createdAtEpochDay", createdAtEpochDay)
}

/* -------------------- IMPORT -------------------- */

internal fun JSONArray?.toCategoryEntities(): List<CategoryEntity> {
    if (this == null) return emptyList()
    val out = ArrayList<CategoryEntity>(length())
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        out.add(
            CategoryEntity(
                id = o.optLong("id", 0L),
                name = o.optString("name", ""),
                emoji = o.optString("emoji", "")
            )
        )
    }
    return out
}

internal fun JSONArray?.toTransactionEntities(): List<TransactionEntity> {
    if (this == null) return emptyList()
    val out = ArrayList<TransactionEntity>(length())
    for (i in 0 until length()) {
        val o = getJSONObject(i)

        val type = o.optString("type", TransactionType.EXPENSE.name).toTransactionTypeOrDefault()

        out.add(
            TransactionEntity(
                id = o.optLong("id", 0L),
                type = type,
                amountCents = o.optLong("amountCents", 0L),
                categoryId = o.optLong("categoryId", 0L),
                note = o.optString("note", ""),
                epochDay = o.optLong("epochDay", 0L),
                recurringId = o.optLongOrNull("recurringId"),
                recurringRunDateEpoch = o.optLongOrNull("recurringRunDateEpoch")
            )
        )
    }
    return out
}

internal fun JSONArray?.toBudgetEntities(): List<BudgetEntity> {
    if (this == null) return emptyList()
    val out = ArrayList<BudgetEntity>(length())
    for (i in 0 until length()) {
        val o = getJSONObject(i)
        out.add(
            BudgetEntity(
                id = o.optLong("id", 0L),
                categoryId = o.optLong("categoryId", 0L),
                limitCents = o.optLong("limitCents", 0L),
                month = o.optInt("month", 1),
                year = o.optInt("year", 1970)
            )
        )
    }
    return out
}

internal fun JSONArray?.toRecurringEntities(): List<RecurringTransactionEntity> {
    if (this == null) return emptyList()
    val out = ArrayList<RecurringTransactionEntity>(length())
    for (i in 0 until length()) {
        val o = getJSONObject(i)

        val title = o.optString("title").ifBlank { o.optString("note", "") }
        val startEpochDay = o.optLong("startEpochDay", 0L)

        out.add(
            RecurringTransactionEntity(
                id = o.optLong("id", 0L),
                title = title,
                type = o.optString("type", TransactionType.EXPENSE.name),
                amountCents = o.optLong("amountCents", 0L),
                categoryId = o.optLongOrNull("categoryId"),
                note = o.optString("note", "").ifBlank { null },

                frequencyDays = o.optInt("frequencyDays", 30),
                startEpochDay = startEpochDay,
                nextDueEpochDay = o.optLong("nextDueEpochDay", startEpochDay),

                isActive = o.optBoolean("isActive", true),
                remindDailyIfOverdue = o.optBoolean("remindDailyIfOverdue", false),
                lastReminderEpochDay = o.optLongOrNull("lastReminderEpochDay"),

                createdAtEpochDay = o.optLong("createdAtEpochDay", startEpochDay)
            )
        )
    }
    return out
}

/* -------------------- small helpers -------------------- */

internal fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}

private fun String.toTransactionTypeOrDefault(): TransactionType {
    return try {
        TransactionType.valueOf(this)
    } catch (_: Throwable) {
        TransactionType.EXPENSE
    }
}
