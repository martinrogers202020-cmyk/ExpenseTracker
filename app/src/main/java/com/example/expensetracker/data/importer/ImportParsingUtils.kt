package com.example.expensetracker.data.importer

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.util.Locale

object ImportParsingUtils {
    private val autoDateParsers: List<DateTimeFormatter> = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("M/d/uuuu").withResolverStyle(ResolverStyle.SMART),
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

    fun parseDate(input: String, formatOverride: String? = null): LocalDate? {
        val s = input.trim()
        if (s.isEmpty()) return null
        if (!formatOverride.isNullOrBlank()) {
            return try {
                LocalDate.parse(s, DateTimeFormatter.ofPattern(formatOverride).withResolverStyle(ResolverStyle.SMART))
            } catch (_: Throwable) {
                null
            }
        }
        for (fmt in autoDateParsers) {
            try {
                return LocalDate.parse(s, fmt)
            } catch (_: Throwable) {
            }
        }
        return null
    }

    fun parseAmount(input: String, separator: DecimalSeparator): BigDecimal? {
        var s = input.trim()
        if (s.isEmpty()) return null
        var negative = false

        if (s.startsWith("(") && s.endsWith(")")) {
            negative = true
            s = s.substring(1, s.length - 1).trim()
        }

        s = s.replace(Regex("[\u00A0\s]"), "")
        s = s.replace("$", "")
        s = s.replace("€", "")
        s = s.replace("£", "")
        s = s.replace("¥", "")
        s = s.replace("₹", "")

        if (s.startsWith("-")) {
            negative = true
            s = s.removePrefix("-")
        } else if (s.startsWith("+")) {
            s = s.removePrefix("+")
        }

        val normalized = when (separator) {
            DecimalSeparator.DOT -> {
                s.replace(",", "")
            }
            DecimalSeparator.COMMA -> {
                s.replace(".", "").replace(",", ".")
            }
        }

        val value = normalized.toBigDecimalOrNull() ?: return null
        return if (negative) value.negate() else value
    }

    fun normalizeAmountToCents(amount: BigDecimal): Long {
        val scaled = amount.setScale(2, RoundingMode.HALF_UP)
        return scaled.movePointRight(2).longValueExact()
    }

    fun buildBankProfileKey(format: ImportFormat, header: List<String>): String {
        val normalized = header.joinToString("|") { it.trim().lowercase(Locale.US) }
        val raw = "${format.name}|$normalized"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun splitCsvLine(line: String): List<String> {
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

    fun normalizeHeader(value: String): String {
        return value.trim()
            .lowercase(Locale.US)
            .replace("\uFEFF", "")
            .replace("_", " ")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
    }

    fun parseMappedRows(
        rows: List<List<String>>,
        mapping: ColumnMapping,
        profileKey: String,
        defaultDescription: String
    ): List<ImportedTxn> {
        val out = ArrayList<ImportedTxn>()
        for (row in rows) {
            if (row.all { it.isBlank() }) continue
            val dateText = row.getOrNull(mapping.dateColumn)?.trim().orEmpty()
            val date = parseDate(dateText, mapping.dateFormat) ?: continue
            val descText = mapping.descriptionColumn?.let { row.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val description = descText.ifBlank { defaultDescription }

            val signedAmount = extractSignedAmount(row, mapping)
            if (signedAmount == null || signedAmount.compareTo(BigDecimal.ZERO) == 0) continue

            out += ImportedTxn(
                date = date,
                description = description,
                amount = signedAmount,
                currency = null,
                extras = mapOf("profileKey" to profileKey)
            )
        }
        return out
    }

    private fun extractSignedAmount(row: List<String>, mapping: ColumnMapping): BigDecimal? {
        val separator = mapping.decimalSeparator
        if (mapping.usesDebitCredit()) {
            val debitText = mapping.debitColumn?.let { row.getOrNull(it)?.trim().orEmpty() }.orEmpty()
            val creditText = mapping.creditColumn?.let { row.getOrNull(it)?.trim().orEmpty() }.orEmpty()

            val credit = parseAmount(creditText, separator)
            if (credit != null && credit.compareTo(BigDecimal.ZERO) != 0) return credit.abs()

            val debit = parseAmount(debitText, separator)
            if (debit != null && debit.compareTo(BigDecimal.ZERO) != 0) return debit.abs().negate()
        }

        val amountText = mapping.amountColumn?.let { row.getOrNull(it)?.trim().orEmpty() }.orEmpty()
        val amount = parseAmount(amountText, separator) ?: return null

        if (amount >= BigDecimal.ZERO && mapping.typeColumn != null) {
            val typeText = row.getOrNull(mapping.typeColumn)?.trim().orEmpty().lowercase(Locale.US)
            if (typeText.contains("debit") || typeText.contains("withdraw") || typeText.contains("expense") || typeText.contains("out")) {
                return amount.abs().negate()
            }
            if (typeText.contains("credit") || typeText.contains("deposit") || typeText.contains("income") || typeText.contains("in")) {
                return amount.abs()
            }
        }

        return amount
    }
}
