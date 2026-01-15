package com.example.expensetracker.data.importer

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.util.Locale

enum class ImportFormat {
    CSV,
    XLSX,
    OFX,
    QIF,
    PDF
}

data class FormatDetection(
    val format: ImportFormat,
    val reason: String
)

object FormatDetector {

    fun detect(context: Context, uri: Uri, displayName: String? = null): FormatDetection {
        val name = displayName ?: uri.lastPathSegment.orEmpty()
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)

        val sniff = sniffBytes(context, uri)
        val sniffText = sniff.text

        if (sniff.bytes.startsWithPdf()) {
            return FormatDetection(ImportFormat.PDF, "PDF signature")
        }
        if (sniff.bytes.startsWithZip()) {
            return FormatDetection(ImportFormat.XLSX, "ZIP container (XLSX)")
        }
        if (sniffText.contains("OFXHEADER", ignoreCase = true) || sniffText.contains("<OFX", ignoreCase = true)) {
            return FormatDetection(ImportFormat.OFX, "OFX header detected")
        }
        if (sniffText.trimStart().startsWith("!Type", ignoreCase = true)) {
            return FormatDetection(ImportFormat.QIF, "QIF header detected")
        }

        return when (ext) {
            "xlsx" -> FormatDetection(ImportFormat.XLSX, "File extension .xlsx")
            "ofx", "qfx" -> FormatDetection(ImportFormat.OFX, "File extension .ofx/.qfx")
            "qif" -> FormatDetection(ImportFormat.QIF, "File extension .qif")
            "pdf" -> FormatDetection(ImportFormat.PDF, "File extension .pdf")
            "csv" -> FormatDetection(ImportFormat.CSV, "File extension .csv")
            else -> FormatDetection(ImportFormat.CSV, "Defaulted to CSV")
        }
    }

    private data class SniffResult(val bytes: ByteArray, val text: String)

    private fun sniffBytes(context: Context, uri: Uri): SniffResult {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffered = BufferedInputStream(input)
                buffered.mark(2048)
                val buffer = ByteArray(2048)
                val read = buffered.read(buffer)
                buffered.reset()
                val bytes = if (read > 0) buffer.copyOf(read) else ByteArray(0)
                val text = bytes.toString(Charsets.UTF_8)
                SniffResult(bytes, text)
            } ?: SniffResult(ByteArray(0), "")
        } catch (_: Throwable) {
            SniffResult(ByteArray(0), "")
        }
    }

    private fun ByteArray.startsWithPdf(): Boolean {
        if (size < 4) return false
        return this[0] == '%'.code.toByte() && this[1] == 'P'.code.toByte() && this[2] == 'D'.code.toByte() && this[3] == 'F'.code.toByte()
    }

    private fun ByteArray.startsWithZip(): Boolean {
        if (size < 2) return false
        return this[0] == 'P'.code.toByte() && this[1] == 'K'.code.toByte()
    }
}
