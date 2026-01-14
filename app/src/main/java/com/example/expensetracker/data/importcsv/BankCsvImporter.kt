package com.example.expensetracker.data.importcsv

import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.data.rules.MerchantRuleEngine
import java.io.BufferedReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs

data class BankCsvRow(
    val date: LocalDate,
    val amountCents: Long,
    val description: String
)

data class BankCsvParseResult(
    val rows: List<BankCsvRow>,
    val warnings: List<String>
)

object BankCsvImporter {

    private val DATE_HEADERS = setOf("date", "transaction date", "booking date", "posted date", "value date")
    private val AMOUNT_HEADERS = setOf("amount", "value", "transaction amount", "amt", "sum")
    private val DESC_HEADERS = setOf("description", "details", "memo", "narration", "merchant", "transaction details", "text")

    private val DATE_FORMATS = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy")
    )

    fun parse(reader: BufferedReader): BankCsvParseResult {
        val warnings = mutableListOf<String>()
        val lines = reader.readLines().filter { it.isNotBlank() }

        if (lines.isEmpty()) return BankCsvParseResult(emptyList(), listOf("CSV is empty."))

        val delimiter = guessDelimiter(lines.take(10))

        val header = parseCsvLine(lines.first(), delimiter).map { it.trim() }
        val headerLower = header.map { it.lowercase(Locale.ROOT).trim() }

        val dateIdx = indexOfAny(headerLower, DATE_HEADERS)
        val amountIdx = indexOfAny(headerLower, AMOUNT_HEADERS)
        val descIdx = indexOfAny(headerLower, DESC_HEADERS)

        if (dateIdx == -1) warnings += "Could not find a date column. Expected one of: ${DATE_HEADERS.joinToString()}."
        if (amountIdx == -1) warnings += "Could not find an amount column. Expected one of: ${AMOUNT_HEADERS.joinToString()}."
        if (descIdx == -1) warnings += "Could not find a description column. Expected one of: ${DESC_HEADERS.joinToString()}."

        if (dateIdx == -1 || amountIdx == -1 || descIdx == -1) {
            return BankCsvParseResult(emptyList(), warnings)
        }

        val out = ArrayList<BankCsvRow>(maxOf(0, lines.size - 1))

        for (lineIndex in 1 until lines.size) {
            val cols = parseCsvLine(lines[lineIndex], delimiter)
            val rowNo = lineIndex + 1

            if (dateIdx >= cols.size || amountIdx >= cols.size || descIdx >= cols.size) {
                warnings += "Row $rowNo: missing required columns — skipped."
                continue
            }

            val rawDate = cols[dateIdx].trim()
            val rawAmount = cols[amountIdx].trim()
            val rawDesc = cols[descIdx].trim()

            val date = parseDate(rawDate)
            if (date == null) {
                warnings += "Row $rowNo: could not parse date '$rawDate' — skipped."
                continue
            }

            val amountCents = parseAmountToCents(rawAmount)
            if (amountCents == null) {
                warnings += "Row $rowNo: could not parse amount '$rawAmount' — skipped."
                continue
            }

            out += BankCsvRow(
                date = date,
                amountCents = amountCents,
                description = rawDesc.ifBlank { "Bank import" }
            )
        }

        return BankCsvParseResult(out, warnings)
    }

    suspend fun toTransactionEntities(
        rows: List<BankCsvRow>,
        defaultCategoryId: Long,
        ruleEngine: MerchantRuleEngine
    ): List<TransactionEntity> {
        return rows.map { r ->
            val resolvedCategory = ruleEngine.resolveCategory(r.description) ?: defaultCategoryId

            TransactionEntity(
                id = 0L,
                type = if (r.amountCents < 0) TransactionType.EXPENSE else TransactionType.INCOME,
                amountCents = abs(r.amountCents),
                categoryId = resolvedCategory,
                note = r.description,
                epochDay = r.date.toEpochDay()
            )
        }
    }

    fun stableKey(epochDay: Long, type: TransactionType, amountCents: Long, note: String): String {
        return "${epochDay}|${type.name}|${amountCents}|${note.trim().lowercase(Locale.ROOT)}"
    }

    private fun guessDelimiter(lines: List<String>): Char {
        val candidates = listOf(',', ';', '\t', '|')
        fun score(d: Char): Int = lines.sumOf { parseCsvLine(it, d).size }
        return candidates.maxBy { score(it) }
    }

    private fun indexOfAny(headersLower: List<String>, aliases: Set<String>): Int {
        for ((i, h) in headersLower.withIndex()) {
            if (aliases.contains(h.trim())) return i
        }
        for ((i, h) in headersLower.withIndex()) {
            val t = h.trim()
            if (aliases.any { a -> t.contains(a) }) return i
        }
        return -1
    }

    private fun parseDate(s: String): LocalDate? {
        val t = s.trim()
        if (t.isBlank()) return null

        for (fmt in DATE_FORMATS) {
            try {
                return LocalDate.parse(t, fmt)
            } catch (_: DateTimeParseException) {}
        }

        if (t.length >= 10) {
            val isoDate = t.take(10)
            return try {
                LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (_: Throwable) {
                null
            }
        }
        return null
    }

    private fun parseAmountToCents(s: String): Long? {
        var t = s.trim()
        if (t.isEmpty()) return null

        var negative = false
        if (t.startsWith("(") && t.endsWith(")")) {
            negative = true
            t = t.substring(1, t.length - 1).trim()
        }
        if (t.startsWith("-")) {
            negative = true
            t = t.removePrefix("-").trim()
        }

        t = t.replace(Regex("[^0-9,\\.\\s]"), "")
        t = t.replace(" ", "")

        val lastDot = t.lastIndexOf('.')
        val lastComma = t.lastIndexOf(',')

        val normalized = when {
            lastDot != -1 && lastComma != -1 ->
                if (lastDot > lastComma) t.replace(",", "") else t.replace(".", "").replace(",", ".")
            lastComma != -1 -> t.replace(".", "").replace(",", ".")
            else -> t.replace(",", "")
        }

        return try {
            val bd = BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
            val cents = bd.multiply(BigDecimal(100)).toLong()
            if (negative) -cents else cents
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    out.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}
