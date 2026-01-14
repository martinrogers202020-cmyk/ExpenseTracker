package com.example.expensetracker.util

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale
import kotlin.math.abs

data class BankCsvRow(
    val date: LocalDate,
    val description: String,
    val amountCentsAbs: Long,
    val typeUpper: String,     // "INCOME" or "EXPENSE"
    val rawLineIndex: Int
)

data class BankCsvParseResult(
    val rows: List<BankCsvRow>,
    val warnings: List<String>
)

object BankCsvParser {

    fun parse(text: String): BankCsvParseResult {
        val lines = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")

        return parseLines(lines)
    }

    fun parseFromUri(context: Context, uri: Uri): BankCsvParseResult {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val br = BufferedReader(InputStreamReader(input))
                val lines = br.readLines()
                parseLines(lines)
            } ?: BankCsvParseResult(emptyList(), listOf("Unable to open file."))
        } catch (t: Throwable) {
            BankCsvParseResult(emptyList(), listOf("Unable to open file: ${t.message}"))
        }
    }

    fun parseLines(lines: List<String>): BankCsvParseResult {
        if (lines.isEmpty()) return BankCsvParseResult(emptyList(), listOf("File is empty."))

        val warnings = mutableListOf<String>()

        val normalizedLines = lines.toMutableList()
        if (normalizedLines.isNotEmpty()) {
            normalizedLines[0] = normalizedLines[0].removePrefix("\uFEFF")
        }

        // ----- Find header row (best effort) -----
        val headerScanLimit = minOf(20, normalizedLines.size)
        var headerIndex = -1
        var headerCols: List<String> = emptyList()
        var bestScore = -1

        for (i in 0 until headerScanLimit) {
            val cols = splitCsvLine(normalizedLines[i])
            val score = headerScore(cols)
            if (score > bestScore) {
                bestScore = score
                headerIndex = i
                headerCols = cols
            }
        }

        val hasHeader = bestScore >= 2 && headerIndex >= 0

        val idx = if (hasHeader) {
            val map = indexMap(headerCols)
            ColumnIndex(
                date = map["date"] ?: map["transaction date"] ?: map["posting date"],
                desc = map["description"]
                    ?: map["descript"]
                    ?: map["details"]
                    ?: map["merchant"]
                    ?: map["narration"]
                    ?: map["memo"]
                    ?: map["payee"],
                type = map["type"] ?: map["transaction type"],
                amount = map["amount"]
                    ?: map["amount $"]
                    ?: map["amount ($)"]
                    ?: map["amount (usd)"]
                    ?: map["value"],
                debit = map["debit"] ?: map["withdrawal"] ?: map["withdrawals"] ?: map["out"] ?: map["paid out"],
                credit = map["credit"] ?: map["deposit"] ?: map["deposits"] ?: map["in"] ?: map["paid in"] ?: map["received"],
                category = map["category"]
            )
        } else {
            // Fallback: Date, Description, Amount
            warnings += "Header not detected reliably. Assuming columns: Date, Description, Amount."
            ColumnIndex(date = 0, desc = 1, type = null, amount = 2, debit = null, credit = null, category = null)
        }

        if (idx.date == null) return BankCsvParseResult(emptyList(), listOf("Couldn't find a Date column."))
        if (idx.desc == null && idx.category == null) warnings += "Couldn't find a Description/Category column. Using blank descriptions where missing."

        val startRow = if (hasHeader) headerIndex + 1 else 0
        val out = ArrayList<BankCsvRow>(maxOf(0, normalizedLines.size - startRow))

        for (lineIndex in startRow until normalizedLines.size) {
            val cols = splitCsvLine(normalizedLines[lineIndex])
            if (cols.all { it.isBlank() }) continue

            val dateText = cols.getOrNull(idx.date)?.trim().orEmpty()
            val parsedDate = parseDate(dateText) ?: continue

            val descText = idx.desc?.let { cols.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val catText = idx.category?.let { cols.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val description = (descText.ifBlank { catText }).ifBlank { "Bank transaction" }.trim()

            val signedCents = parseSignedCents(cols, idx)
            if (signedCents == null || signedCents == 0L) {
                warnings += "Skipped row ${lineIndex + 1}: amount not found/invalid."
                continue
            }

            val typeFromTypeCol = idx.type?.let { cols.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val typeUpper = inferTypeUpper(typeFromTypeCol, signedCents)

            out += BankCsvRow(
                date = parsedDate,
                description = description,
                amountCentsAbs = abs(signedCents),
                typeUpper = typeUpper,
                rawLineIndex = lineIndex
            )
        }

        val rows = out.sortedWith(compareBy<BankCsvRow> { it.date }.thenBy { it.rawLineIndex })
        return BankCsvParseResult(rows, warnings.distinct())
    }

    private data class ColumnIndex(
        val date: Int?,
        val desc: Int?,
        val type: Int?,
        val amount: Int?,
        val debit: Int?,
        val credit: Int?,
        val category: Int?
    )

    private fun inferTypeUpper(typeText: String, signedCents: Long): String {
        val t = typeText.trim().lowercase(Locale.US)
        if (t.isNotEmpty()) {
            if (t.contains("income") || t.contains("credit") || t.contains("deposit") || t.contains("in")) return "INCOME"
            if (t.contains("expense") || t.contains("debit") || t.contains("withdraw") || t.contains("out")) return "EXPENSE"
        }
        return if (signedCents < 0) "EXPENSE" else "INCOME"
    }

    private fun parseSignedCents(cols: List<String>, idx: ColumnIndex): Long? {
        // 1) If Debit/Credit exist, prefer them
        val debitText = idx.debit?.let { cols.getOrNull(it).orEmpty() }?.trim().orEmpty()
        val creditText = idx.credit?.let { cols.getOrNull(it).orEmpty() }?.trim().orEmpty()
        val hasDebitCredit = (idx.debit != null || idx.credit != null)

        if (hasDebitCredit) {
            val debitCents = parseMoneyToSignedCents(debitText)
            val creditCents = parseMoneyToSignedCents(creditText)

            if (creditCents != null && creditCents != 0L) return +abs(creditCents)
            if (debitCents != null && debitCents != 0L) return -abs(debitCents)
        }

        // 2) Single Amount column (supports negative numbers)
        val amountText = idx.amount?.let { cols.getOrNull(it).orEmpty() }?.trim().orEmpty()
        val cents = parseMoneyToSignedCents(amountText)
        if (cents != null && cents != 0L) return cents

        // 3) Last resort: scan any column that looks like money
        for (c in cols) {
            val v = parseMoneyToSignedCents(c.trim())
            if (v != null && v != 0L) return v
        }

        return null
    }

    /* ========================= CSV SPLIT ========================= */

    private fun splitCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]
            when (ch) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) sb.append(ch) else {
                        out.add(sb.toString())
                        sb.setLength(0)
                    }
                }
                else -> sb.append(ch)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }

    /* ========================= HEADER DETECTION ========================= */

    private fun headerScore(cols: List<String>): Int {
        val keys = cols.map { normalizeKey(it) }.toSet()
        var score = 0
        if ("date" in keys || "transaction date" in keys || "posting date" in keys) score++
        if (
            "description" in keys || "descript" in keys || "details" in keys ||
            "narration" in keys || "merchant" in keys || "memo" in keys || "payee" in keys
        ) score++
        if ("type" in keys || "transaction type" in keys) score++
        if ("amount" in keys || "amount $" in keys || "amount ($)" in keys || "amount (usd)" in keys || "value" in keys) score++
        if ("debit" in keys || "withdrawal" in keys || "withdrawals" in keys || "out" in keys || "paid out" in keys) score++
        if ("credit" in keys || "deposit" in keys || "deposits" in keys || "in" in keys || "paid in" in keys || "received" in keys) score++
        if ("category" in keys) score++
        return score
    }

    private fun indexMap(cols: List<String>): Map<String, Int> {
        val m = HashMap<String, Int>()
        cols.forEachIndexed { i, raw ->
            val k = normalizeKey(raw)
            if (k.isNotBlank() && !m.containsKey(k)) m[k] = i
        }
        return m
    }

    private fun normalizeKey(s: String): String {
        return s.trim()
            .lowercase(Locale.US)
            .replace("\uFEFF", "")
            .replace("_", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
    }

    /* ========================= DATE PARSING ========================= */

    private val dateParsers: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE, // 2026-01-12
        DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.SMART),  // 1/6/2026
        DateTimeFormatter.ofPattern("MM/d/uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatter.ofPattern("M/dd/uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d MMM uuuu")
            .toFormatter(Locale.US).withResolverStyle(ResolverStyle.SMART),
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-uuuu")
            .toFormatter(Locale.US).withResolverStyle(ResolverStyle.SMART)
    )

    private fun parseDate(input: String): LocalDate? {
        val s = input.trim()
        if (s.isEmpty()) return null
        for (fmt in dateParsers) {
            try {
                return LocalDate.parse(s, fmt)
            } catch (_: Throwable) {
            }
        }
        return null
    }

    /* ========================= MONEY PARSING ========================= */

    private fun parseMoneyToSignedCents(input: String): Long? {
        var s = input.trim()
        if (s.isEmpty()) return null

        var negative = false

        // Parentheses negative: (123.45)
        if (s.startsWith("(") && s.endsWith(")")) {
            negative = true
            s = s.substring(1, s.length - 1).trim()
        }

        // Remove spaces + currency
        s = s.replace(Regex("[\\s\\u00A0]"), "")
        s = s.replace("$", "")
        s = s.replace("€", "")
        s = s.replace("£", "")
        s = s.replace("¥", "")
        s = s.replace("₹", "")

        // Leading sign
        if (s.startsWith("-")) {
            negative = true
            s = s.removePrefix("-")
        } else if (s.startsWith("+")) {
            s = s.removePrefix("+")
        }

        // Remove thousands separators
        s = s.replace(",", "")

        if (s.isEmpty()) return null

        val parts = s.split(".")
        val major = parts.getOrNull(0)?.toLongOrNull() ?: return null
        val minor = when (parts.size) {
            1 -> 0L
            else -> {
                val frac = parts[1]
                when {
                    frac.isEmpty() -> 0L
                    frac.length == 1 -> (frac + "0").toLongOrNull() ?: return null
                    else -> frac.take(2).toLongOrNull() ?: return null
                }
            }
        }

        val cents = major * 100L + minor
        return if (negative) -cents else cents
    }

    private fun <T> List<T>.getOrNull(index: Int?): T? {
        if (index == null) return null
        if (index < 0 || index >= size) return null
        return this[index]
    }
}
