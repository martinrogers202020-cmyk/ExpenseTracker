package com.example.expensetracker.data.importer

import java.math.BigDecimal
import java.time.LocalDate


data class ImportedTxn(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val currency: String? = null,
    val extras: Map<String, String> = emptyMap()
)

interface Importer {
    suspend fun import(context: android.content.Context, source: ImportSource, mapping: ColumnMapping? = null): ImporterResult
}

data class ImportSource(
    val uri: android.net.Uri,
    val displayName: String? = null
)

data class ImporterResult(
    val transactions: List<ImportedTxn>,
    val diagnostics: ImporterDiagnostics = ImporterDiagnostics()
)

data class ImporterDiagnostics(
    val warnings: List<String> = emptyList(),
    val needsMapping: Boolean = false,
    val columns: List<String> = emptyList(),
    val sampleRows: List<List<String>> = emptyList(),
    val bankProfileKey: String? = null,
    val suggestedMapping: ColumnMapping? = null,
    val pdfTextUncertain: Boolean = false,
    val scannedPdfDetected: Boolean = false
)

enum class DecimalSeparator {
    DOT,
    COMMA
}

data class ColumnMapping(
    val dateColumn: Int,
    val descriptionColumn: Int?,
    val typeColumn: Int?,
    val amountColumn: Int?,
    val debitColumn: Int?,
    val creditColumn: Int?,
    val dateFormat: String?,
    val decimalSeparator: DecimalSeparator
) {
    fun usesDebitCredit(): Boolean = debitColumn != null || creditColumn != null

    fun isValid(): Boolean {
        if (dateColumn < 0) return false
        val hasAmount = amountColumn != null
        val hasDebitCredit = debitColumn != null || creditColumn != null
        return hasAmount || hasDebitCredit
    }
}
