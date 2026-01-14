package com.example.expensetracker.util

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ExportUtils {

    fun buildReportsCsv(
        title: String,
        rows: List<List<String>>
    ): String {
        val sb = StringBuilder()
        sb.appendLine(title)
        sb.appendLine("Generated,${nowStamp()}")
        sb.appendLine()

        // header
        sb.appendLine("Date,Type,Amount,Note")

        // data
        rows.forEach { cols ->
            // Date, Type, Amount, Note
            val safe = cols.map { escapeCsv(it) }
            sb.appendLine(safe.joinToString(","))
        }

        return sb.toString()
    }

    fun writeTextToUri(context: Context, uri: Uri, text: String) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            OutputStreamWriter(out).use { writer ->
                writer.write(text)
            }
        }
    }

    fun writeSimpleTablePdfToUri(
        context: Context,
        uri: Uri,
        title: String,
        header: List<String>,
        rows: List<List<String>>
    ) {
        val doc = PdfDocument()

        val pageWidth = 595   // A4-ish
        val pageHeight = 842  // A4-ish

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val left = 40f
        val top = 50f
        val lineGap = 16f

        // Column widths for: Date, Type, Amount, Note
        val colWidths = floatArrayOf(90f, 70f, 90f, 260f)

        var pageNumber = 1
        var y = top

        fun newPage(): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = doc.startPage(pageInfo)

            val canvas = page.canvas
            y = top

            canvas.drawText(title, left, y, titlePaint)
            y += lineGap

            canvas.drawText("Generated: ${nowStamp()}", left, y, smallPaint)
            y += (lineGap + 6f)

            // header row
            var x = left
            header.forEachIndexed { idx, h ->
                canvas.drawText(h, x, y, headerPaint)
                x += colWidths[idx]
            }
            y += lineGap

            // separator
            canvas.drawLine(left, y, (pageWidth - left), y, smallPaint)
            y += lineGap

            return page
        }

        var page = newPage()

        fun finishPage() {
            doc.finishPage(page)
            pageNumber += 1
        }

        val maxY = pageHeight - 60f

        rows.forEach { row ->
            if (y > maxY) {
                finishPage()
                page = newPage()
            }

            var x = left
            row.forEachIndexed { idx, cell ->
                val text = cell.take(80) // keep it readable
                page.canvas.drawText(text, x, y, smallPaint)
                x += colWidths[idx]
            }
            y += lineGap
        }

        finishPage()

        context.contentResolver.openOutputStream(uri)?.use { out ->
            doc.writeTo(out)
        }

        doc.close()
    }

    private fun nowStamp(): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return LocalDateTime.now().format(fmt)
    }

    private fun escapeCsv(value: String): String {
        val v = value.replace("\n", " ").replace("\r", " ").trim()
        val needsQuotes = v.contains(",") || v.contains("\"")
        val escaped = v.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
