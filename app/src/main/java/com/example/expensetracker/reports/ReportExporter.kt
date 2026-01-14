package com.example.expensetracker.reports

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.abs

data class ReportRow(
    val date: LocalDate,
    val year: Int,
    val categoryName: String,
    val typeLabel: String,        // "Income" | "Expense"
    val description: String,      // from note
    val amountSignedCents: Long,  // income positive, expense negative
    val txId: Long
)

object ReportExporter {

    /**
     * Build rows exactly like:
     * Date | Year | Category | Type | Description | Amount ($)
     */
    fun buildRows(
        txs: List<TransactionEntity>,
        categoryNameById: Map<Long, String>
    ): List<ReportRow> {
        return txs
            .sortedWith(compareBy<TransactionEntity> { it.epochDay }.thenBy { it.id })
            .map { tx ->
                val date = LocalDate.ofEpochDay(tx.epochDay)
                val catName = categoryNameById[tx.categoryId] ?: "Unknown"

                val typeLabel = if (tx.type == TransactionType.INCOME) "Income" else "Expense"

                // Keep description clean (don’t leak attachment line into the main description)
                val desc = tx.note
                    .lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() && !it.startsWith("Attachment:", ignoreCase = true) }
                    ?: ""

                // Your DB stores amountCents as positive; make expenses negative for reporting.
                val signedCents =
                    if (tx.type == TransactionType.EXPENSE) -abs(tx.amountCents) else abs(tx.amountCents)

                ReportRow(
                    date = date,
                    year = date.year,
                    categoryName = catName,
                    typeLabel = typeLabel,
                    description = desc,
                    amountSignedCents = signedCents,
                    txId = tx.id
                )
            }
    }

    /**
     * CSV matching the screenshot:
     * Date,Year,Category,Type,Description,Amount ($)
     *
     * Amount examples:
     *  300.00
     * -45.75
     */
    fun toCsv(rows: List<ReportRow>): String {
        val sb = StringBuilder()
        sb.append("Date,Year,Category,Type,Description,Amount ($)\n")

        for (r in rows) {
            sb.append(esc(r.date.toString())).append(',')
            sb.append(esc(r.year.toString())).append(',')
            sb.append(esc(r.categoryName)).append(',')
            sb.append(esc(r.typeLabel)).append(',')
            sb.append(esc(r.description)).append(',')
            sb.append(esc(formatDollars(r.amountSignedCents)))
                .append('\n')
        }

        return sb.toString()
    }

    /**
     * Writes CSV into app external files directory (Documents, no permission needed).
     */
    fun writeCsvFile(context: Context, fileName: String, csv: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(dir, fileName)
        file.writeText(csv)
        return file
    }

    /**
     * PDF table matching the screenshot:
     * Date | Year | Category | Type | Description | Amount ($)
     */
    fun writePdfFile(context: Context, fileName: String, rows: List<ReportRow>): File {
        val doc = PdfDocument()

        // A4-ish in points
        val pageWidth = 595
        val pageHeight = 842
        val margin = 28f
        val bottomMargin = 32f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1f
            color = android.graphics.Color.argb(60, 0, 0, 0)
        }

        // Column X positions (tuned to fit A4)
        val xDate = margin
        val xYear = margin + 92f
        val xCategory = margin + 145f
        val xType = margin + 250f
        val xDesc = margin + 320f
        val xAmountRight = pageWidth - margin

        val rowHeight = 18f

        var pageNumber = 1
        var y = margin

        fun startPage(): PdfDocument.Page {
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            pageNumber += 1
            y = margin
            return doc.startPage(info)
        }

        fun drawHeader(canvas: android.graphics.Canvas, subtitle: String?) {
            // Title
            canvas.drawText("ExpenseTracker Report", margin, y + 16f, titlePaint)
            y += 26f

            subtitle?.let {
                canvas.drawText(it, margin, y + 12f, cellPaint)
                y += 18f
            }

            // Column header row
            canvas.drawLine(margin, y + 6f, pageWidth - margin, y + 6f, linePaint)
            y += 16f

            canvas.drawText("Date", xDate, y, headerPaint)
            canvas.drawText("Year", xYear, y, headerPaint)
            canvas.drawText("Category", xCategory, y, headerPaint)
            canvas.drawText("Type", xType, y, headerPaint)
            canvas.drawText("Description", xDesc, y, headerPaint)

            val amountHeader = "Amount (\$)"
            val amountHeaderX = xAmountRight - headerPaint.measureText(amountHeader)
            canvas.drawText(amountHeader, amountHeaderX, y, headerPaint)

            y += 10f
            canvas.drawLine(margin, y + 6f, pageWidth - margin, y + 6f, linePaint)
            y += 16f
        }

        fun ensureSpaceOrNewPage(
            currentPage: PdfDocument.Page,
            subtitle: String?
        ): PdfDocument.Page {
            val bottomLimit = pageHeight - bottomMargin
            return if (y + rowHeight <= bottomLimit) {
                currentPage
            } else {
                doc.finishPage(currentPage)
                val newPage = startPage()
                drawHeader(newPage.canvas, subtitle)
                newPage
            }
        }

        val subtitle = if (rows.isNotEmpty()) {
            val start = rows.first().date
            val end = rows.last().date
            "Period: $start to $end"
        } else {
            "Period: -"
        }

        var page = startPage()
        drawHeader(page.canvas, subtitle)

        for (r in rows) {
            page = ensureSpaceOrNewPage(page, subtitle)
            val canvas = page.canvas

            val amountText = formatDollars(r.amountSignedCents)

            canvas.drawText(r.date.toString(), xDate, y, cellPaint)
            canvas.drawText(r.year.toString(), xYear, y, cellPaint)
            canvas.drawText(ellipsis(r.categoryName, 14), xCategory, y, cellPaint)
            canvas.drawText(r.typeLabel, xType, y, cellPaint)
            canvas.drawText(ellipsis(r.description.replace("\n", " "), 28), xDesc, y, cellPaint)

            val amountX = xAmountRight - cellPaint.measureText(amountText)
            canvas.drawText(amountText, amountX, y, cellPaint)

            y += rowHeight
        }

        doc.finishPage(page)

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(dir, fileName)

        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()

        return file
    }

    // ---------- helpers ----------

    private fun esc(value: String): String {
        val v = value.replace("\"", "\"\"")
        return "\"$v\""
    }

    private fun formatDollars(signedCents: Long): String {
        val sign = if (signedCents < 0) "-" else ""
        val absCents = abs(signedCents)
        val major = absCents / 100
        val minor = absCents % 100
        return sign + major.toString() + "." + minor.toString().padStart(2, '0')
    }

    private fun ellipsis(s: String, max: Int): String {
        val t = s.trim()
        if (t.length <= max) return t
        if (max <= 1) return t.take(max)
        return t.take(max - 1) + "…"
    }
}
