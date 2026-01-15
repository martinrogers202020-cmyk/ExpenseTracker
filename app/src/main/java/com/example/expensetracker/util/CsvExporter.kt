package com.example.expensetracker.util

import android.content.Context
import com.example.expensetracker.R

data class ExportRow(
    val date: String,
    val type: String,
    val category: String,
    val note: String,
    val amount: String
)

object CsvExporter {

    private fun esc(value: String): String {
        val v = value.replace("\"", "\"\"")
        return "\"$v\""
    }

    fun toCsv(context: Context, rows: List<ExportRow>): String {
        val header = context.getString(R.string.export_csv_header_simple_lowercase)
        val body = rows.joinToString("\n") { r ->
            listOf(r.date, r.type, r.category, r.note, r.amount)
                .joinToString(",") { esc(it) }
        }
        return header + "\n" + body
    }
}
