package com.example.expensetracker.util

import com.example.expensetracker.data.model.TransactionType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private val moneyFmt: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    fun money(cents: Long): String {
        return moneyFmt.format(cents / 100.0)
    }

    fun signedMoney(cents: Long, type: TransactionType): String {
        val base = money(cents)
        return if (type == TransactionType.EXPENSE) "-$base" else base
    }

    fun dateFromEpochDay(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(dateFmt)
    }
}
