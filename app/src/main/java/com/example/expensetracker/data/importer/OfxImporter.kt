package com.example.expensetracker.data.importer

import android.content.Context
import com.example.expensetracker.R
import java.time.LocalDate

class OfxImporter : Importer {
    override suspend fun import(context: Context, source: ImportSource, mapping: ColumnMapping?): ImporterResult {
        val text = try {
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            } ?: ""
        } catch (_: Throwable) {
            ""
        }

        if (text.isBlank()) {
            return ImporterResult(emptyList())
        }

        val currency = Regex("<CURDEF>([^<\n]+)", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)
        val blocks = Regex(
            "<STMTTRN>(.*?)</STMTTRN>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
            .findAll(text)
            .map { it.groupValues[1] }
            .toList()

        val transactions = ArrayList<ImportedTxn>()
        val defaultDescription = context.getString(R.string.csv_import_bank_transaction)
        for (block in blocks) {
            val dateRaw = findTag(block, "DTPOSTED") ?: findTag(block, "DTUSER")
            val amountRaw = findTag(block, "TRNAMT")
            if (dateRaw == null || amountRaw == null) continue

            val date = parseOfxDate(dateRaw) ?: continue
            val amount = ImportParsingUtils.parseAmount(amountRaw, DecimalSeparator.DOT) ?: continue

            val memo = findTag(block, "MEMO") ?: findTag(block, "NAME") ?: ""
            val fitId = findTag(block, "FITID")

            transactions += ImportedTxn(
                date = date,
                description = memo.ifBlank { defaultDescription },
                amount = amount,
                currency = currency,
                extras = mapOfNotNull("fitId" to fitId)
            )
        }

        return ImporterResult(transactions)
    }

    private fun findTag(block: String, tag: String): String? {
        val regex = Regex("<$tag>([^<\r\n]+)", RegexOption.IGNORE_CASE)
        return regex.find(block)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun parseOfxDate(raw: String): LocalDate? {
        val digits = raw.trim().take(8)
        if (digits.length < 8) return null
        val year = digits.substring(0, 4).toIntOrNull() ?: return null
        val month = digits.substring(4, 6).toIntOrNull() ?: return null
        val day = digits.substring(6, 8).toIntOrNull() ?: return null
        return LocalDate.of(year, month, day)
    }

    private fun mapOfNotNull(vararg pairs: Pair<String, String?>): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        for (pair in pairs) {
            val value = pair.second
            if (!value.isNullOrBlank()) map[pair.first] = value
        }
        return map
    }
}
