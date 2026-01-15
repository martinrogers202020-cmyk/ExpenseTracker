package com.example.expensetracker.data.importer

import android.content.Context
import com.example.expensetracker.R
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedInputStream

class XlsxImporter : Importer {
    override suspend fun import(context: Context, source: ImportSource, mapping: ColumnMapping?): ImporterResult {
        val formatter = DataFormatter()
        val table = try {
            context.contentResolver.openInputStream(source.uri)?.use { input ->
                BufferedInputStream(input).use { buffered ->
                    val workbook = XSSFWorkbook(buffered)
                    if (workbook.numberOfSheets == 0) {
                        workbook.close()
                        return@use emptyList()
                    }
                    val sheet = workbook.getSheetAt(0)
                    val rows = ArrayList<List<String>>()
                    val lastRow = sheet.lastRowNum
                    for (rowIndex in 0..lastRow) {
                        val row = sheet.getRow(rowIndex) ?: continue
                        val lastCell = row.lastCellNum.toInt().coerceAtLeast(0)
                        val values = ArrayList<String>()
                        for (cellIndex in 0 until lastCell) {
                            val cell = row.getCell(cellIndex)
                            values.add(cell?.let { formatter.formatCellValue(it) }.orEmpty())
                        }
                        rows.add(values)
                    }
                    workbook.close()
                    rows
                }
            } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }

        if (table.isEmpty()) {
            return ImporterResult(
                emptyList(),
                ImporterDiagnostics(warnings = listOf(context.getString(R.string.csv_import_file_empty)))
            )
        }

        val header = table.firstOrNull().orEmpty()
        val dataRows = if (table.size > 1) table.drop(1) else emptyList()
        val columnLabels = header.ifEmpty {
            val maxCols = table.maxOfOrNull { it.size } ?: 0
            List(maxCols) { index -> "Column ${index + 1}" }
        }

        val profileKey = ImportParsingUtils.buildBankProfileKey(ImportFormat.XLSX, columnLabels)
        val autoMapping = if (header.isNotEmpty()) mappingFromHeader(header) else null
        val effectiveMapping = mapping ?: autoMapping

        if (effectiveMapping == null || !effectiveMapping.isValid()) {
            return ImporterResult(
                emptyList(),
                ImporterDiagnostics(
                    warnings = listOf(context.getString(R.string.import_mapping_required)),
                    needsMapping = true,
                    columns = columnLabels,
                    sampleRows = dataRows.take(8),
                    bankProfileKey = profileKey,
                    suggestedMapping = autoMapping
                )
            )
        }

        val txns = ImportParsingUtils.parseMappedRows(
            rows = dataRows,
            mapping = effectiveMapping,
            profileKey = profileKey,
            defaultDescription = context.getString(R.string.csv_import_bank_transaction)
        )

        return ImporterResult(txns, ImporterDiagnostics(bankProfileKey = profileKey))
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
