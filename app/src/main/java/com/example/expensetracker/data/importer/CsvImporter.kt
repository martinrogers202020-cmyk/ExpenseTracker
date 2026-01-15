package com.example.expensetracker.data.importer

import android.content.Context
import com.example.expensetracker.R
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvImporter : Importer {
    override suspend fun import(context: Context, source: ImportSource, mapping: ColumnMapping?): ImporterResult {
        val lines = try {
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                BufferedReader(InputStreamReader(input)).readLines()
            } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }

        if (lines.isEmpty()) {
            return ImporterResult(
                emptyList(),
                ImporterDiagnostics(warnings = listOf(context.getString(R.string.csv_import_file_empty)))
            )
        }

        val normalizedLines = lines.toMutableList()
        normalizedLines[0] = normalizedLines[0].removePrefix("\uFEFF")

        val headerScanLimit = minOf(20, normalizedLines.size)
        var headerIndex = -1
        var headerCols: List<String> = emptyList()
        var bestScore = -1

        for (i in 0 until headerScanLimit) {
            val cols = ImportParsingUtils.splitCsvLine(normalizedLines[i])
            val score = headerScore(cols)
            if (score > bestScore) {
                bestScore = score
                headerIndex = i
                headerCols = cols
            }
        }

        val hasHeader = bestScore >= 2 && headerIndex >= 0
        val dataStart = if (hasHeader) headerIndex + 1 else 0
        val rawRows = normalizedLines.drop(dataStart).map { ImportParsingUtils.splitCsvLine(it) }

        val columnLabels = if (hasHeader) {
            headerCols
        } else {
            val maxCols = rawRows.maxOfOrNull { it.size } ?: 0
            List(maxCols) { index -> "Column ${index + 1}" }
        }

        val profileKey = ImportParsingUtils.buildBankProfileKey(ImportFormat.CSV, columnLabels)

        val autoMapping = if (hasHeader) mappingFromHeader(headerCols) else null
        val effectiveMapping = mapping ?: autoMapping

        if (effectiveMapping == null || !effectiveMapping.isValid()) {
            val samples = rawRows.take(8)
            return ImporterResult(
                emptyList(),
                ImporterDiagnostics(
                    warnings = listOf(context.getString(R.string.import_mapping_required)),
                    needsMapping = true,
                    columns = columnLabels,
                    sampleRows = samples,
                    bankProfileKey = profileKey,
                    suggestedMapping = autoMapping
                )
            )
        }

        val txns = ImportParsingUtils.parseMappedRows(
            rows = rawRows,
            mapping = effectiveMapping,
            profileKey = profileKey,
            defaultDescription = context.getString(R.string.csv_import_bank_transaction)
        )
        return ImporterResult(txns, ImporterDiagnostics(bankProfileKey = profileKey))
    }

    private fun headerScore(cols: List<String>): Int {
        val keys = cols.map { ImportParsingUtils.normalizeHeader(it) }.toSet()
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

    private fun mappingFromHeader(cols: List<String>): ColumnMapping? {
        val map = HashMap<String, Int>()
        cols.forEachIndexed { index, raw ->
            val key = ImportParsingUtils.normalizeHeader(raw)
            if (key.isNotBlank() && !map.containsKey(key)) map[key] = index
        }

        val date = map["date"] ?: map["transaction date"] ?: map["posting date"]
        val desc = map["description"] ?: map["descript"] ?: map["details"] ?: map["merchant"] ?: map["narration"] ?: map["memo"] ?: map["payee"]
        val type = map["type"] ?: map["transaction type"]
        val amount = map["amount"] ?: map["amount $"] ?: map["amount ($)"] ?: map["amount (usd)"] ?: map["value"]
        val debit = map["debit"] ?: map["withdrawal"] ?: map["withdrawals"] ?: map["out"] ?: map["paid out"]
        val credit = map["credit"] ?: map["deposit"] ?: map["deposits"] ?: map["in"] ?: map["paid in"] ?: map["received"]

        if (date == null) return null
        if (amount == null && debit == null && credit == null) return null

        return ColumnMapping(
            dateColumn = date,
            descriptionColumn = desc,
            typeColumn = type,
            amountColumn = amount,
            debitColumn = debit,
            creditColumn = credit,
            dateFormat = null,
            decimalSeparator = DecimalSeparator.DOT
        )
    }
}
