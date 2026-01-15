package com.example.expensetracker.data.importer

import android.content.Context
import com.example.expensetracker.R

class QifImporter : Importer {
    override suspend fun import(context: Context, source: ImportSource, mapping: ColumnMapping?): ImporterResult {
        val text = try {
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        } catch (_: Throwable) {
            ""
        }

        if (text.isBlank()) return ImporterResult(emptyList())

        val transactions = ArrayList<ImportedTxn>()
        val defaultDescription = context.getString(R.string.csv_import_bank_transaction)
        val blocks = text.split("^")
        for (block in blocks) {
            val lines = block.trim().lines()
            if (lines.isEmpty()) continue

            var dateText: String? = null
            var amountText: String? = null
            var payee: String? = null
            var memo: String? = null

            for (line in lines) {
                if (line.isEmpty()) continue
                when (line.firstOrNull()) {
                    'D' -> dateText = line.drop(1).trim()
                    'T' -> amountText = line.drop(1).trim()
                    'P' -> payee = line.drop(1).trim()
                    'M' -> memo = line.drop(1).trim()
                }
            }

            val date = dateText?.let { ImportParsingUtils.parseDate(it, null) } ?: continue
            val amount = amountText?.let { ImportParsingUtils.parseAmount(it, DecimalSeparator.DOT) } ?: continue

            val description = listOfNotNull(payee, memo).joinToString(" ").trim()

            transactions += ImportedTxn(
                date = date,
                description = if (description.isBlank()) defaultDescription else description,
                amount = amount,
                currency = null,
                extras = emptyMap()
            )
        }

        return ImporterResult(transactions)
    }
}
