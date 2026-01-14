package com.example.expensetracker.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object Money {

    /**
     * Converts a user-entered dollar string to cents.
     *
     * Accepts:
     *  "100"
     *  "100.5"
     *  "100.50"
     *  "$1,234.56"
     *  "1,234"
     */
    fun dollarsTextToCents(input: String): Long {
        val cleaned = input.trim()
            .replace("$", "")
            .replace(",", "")
            .replace(" ", "")

        val bd = cleaned.toBigDecimalOrNull() ?: BigDecimal.ZERO

        // dollars -> cents
        return bd.multiply(BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Converts cents to a string like:
     *  10000 -> "100.00"
     *  1050  -> "10.50"
     */
    fun centsToPlainDollars(cents: Long): String {
        val abs = kotlin.math.abs(cents)
        val dollars = abs / 100
        val rem = abs % 100
        val sign = if (cents < 0) "-" else ""
        return "$sign$dollars.${rem.toString().padStart(2, '0')}"
    }

    /**
     * Converts cents to "$1,234.56" style.
     */
    fun centsToCurrency(cents: Long): String {
        val abs = kotlin.math.abs(cents)
        val value = abs / 100.0
        val df = DecimalFormat("#,##0.00")
        val sign = if (cents < 0) "-" else ""
        return "$sign$${df.format(value)}"
    }
}
