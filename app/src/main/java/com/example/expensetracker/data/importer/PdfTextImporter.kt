package com.example.expensetracker.data.importer

import android.content.Context
import com.example.expensetracker.R
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

class PdfTextImporter : Importer {
    override suspend fun import(context: Context, source: ImportSource, mapping: ColumnMapping?): ImporterResult {
        PDFBoxResourceLoader.init(context)
        val text = try {
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                PDDocument.load(input).use { doc ->
                    PDFTextStripper().getText(doc)
                }
            } ?: ""
        } catch (_: Throwable) {
            ""
        }

        if (text.isBlank()) {
            return ImporterResult(
                emptyList(),
                ImporterDiagnostics(
                    warnings = listOf(context.getString(R.string.import_pdf_scanned_warning)),
                    pdfTextUncertain = true,
                    scannedPdfDetected = true
                )
            )
        }

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val defaultDescription = context.getString(R.string.csv_import_bank_transaction)
        val transactions = ArrayList<ImportedTxn>()

        for (line in lines) {
            val parsed = parseLine(line, defaultDescription) ?: continue
            transactions += parsed
        }

        val warnings = listOf(context.getString(R.string.import_pdf_text_warning))
        return ImporterResult(
            transactions,
            ImporterDiagnostics(warnings = warnings, pdfTextUncertain = true)
        )
    }

    private fun parseLine(line: String, defaultDescription: String): ImportedTxn? {
        val dateMatch = dateRegex.find(line) ?: return null
        val amountMatch = amountRegex.findAll(line).lastOrNull() ?: return null

        val dateText = dateMatch.value
        val amountText = amountMatch.value

        val date = ImportParsingUtils.parseDate(dateText, null) ?: return null
        val separator = if (amountText.contains(',') && !amountText.contains('.')) DecimalSeparator.COMMA else DecimalSeparator.DOT
        val amount = ImportParsingUtils.parseAmount(amountText, separator) ?: return null

        val description = line
            .replace(dateText, "")
            .replace(amountText, "")
            .replace(Regex("\s+"), " ")
            .trim()

        return ImportedTxn(
            date = date,
            description = if (description.isBlank()) defaultDescription else description,
            amount = amount,
            currency = null,
            extras = emptyMap()
        )
    }

    companion object {
        private val dateRegex = Regex(
            "\\b(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{1,2}\\.\\d{1,2}\\.\\d{2,4})\\b"
        )
        private val amountRegex = Regex("[-+]?(?:\\d{1,3}(?:[.,]\\d{3})+|\\d+)(?:[.,]\\d{2})?")
    }
}
