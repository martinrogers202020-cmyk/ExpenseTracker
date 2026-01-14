package com.example.expensetracker.util

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

    fun toCsv(rows: List<ExportRow>): String {
        val header = "date,type,category,note,amount"
        val body = rows.joinToString("\n") { r ->
            listOf(r.date, r.type, r.category, r.note, r.amount)
                .joinToString(",") { esc(it) }
        }
        return header + "\n" + body
    }
}
