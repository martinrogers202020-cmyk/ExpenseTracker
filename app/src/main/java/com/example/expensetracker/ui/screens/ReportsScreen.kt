package com.example.expensetracker.ui.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.R
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.ui.state.TransactionItemUi
import com.example.expensetracker.ui.viewmodel.ReportsViewModel
import com.example.expensetracker.ui.viewmodel.ReportsViewModelFactory
import com.example.expensetracker.util.Formatters
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.min

private enum class RangeTab { TODAY, WEEK, MONTH, CUSTOM }
private enum class ExportFormat { CSV, PDF }
private enum class ExportScope { MONTH, YEAR, CUSTOM }

private data class CategorySlice(val label: String, val valueCents: Long)
private data class DailyPoint(val date: LocalDate, val netCents: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    proEnabled: Boolean,
    onOpenPaywall: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val vm: ReportsViewModel = viewModel(factory = ReportsViewModelFactory(appContext))
    val state by vm.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    var tab by rememberSaveable { mutableStateOf(RangeTab.MONTH) }

    val cs = MaterialTheme.colorScheme
    val bg = remember(cs.background) { Brush.verticalGradient(listOf(cs.background, cs.background, cs.background)) }
    val accent = cs.primary
    val border = cs.outlineVariant
    val textPrimary = cs.onSurface
    val textSecondary = cs.onSurfaceVariant
    val expenseColor = cs.error
    val incomeColor = cs.tertiary

    val month: YearMonth = state.month

    val byCategory by remember(state.topExpenseCategories) {
        derivedStateOf { state.topExpenseCategories.map { CategorySlice(it.label, it.amountCents) } }
    }

    val dailyNet by remember(state.transactions, month) {
        derivedStateOf { buildDailyNetPoints(month, state.transactions) }
    }

    // Avoid sorting/filtering on every recomposition
    val monthTxs by remember(state.transactions, month) {
        derivedStateOf { state.transactions.sortedBy { it.epochDay } }
    }

    val pendingCsvText = remember { mutableStateOf<String?>(null) }
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = object : ActivityResultContracts.CreateDocument("text/csv") {
            override fun createIntent(context: Context, input: String) =
                super.createIntent(context, input).apply {
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                }
        },
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val text = pendingCsvText.value ?: return@rememberLauncherForActivityResult
            try {
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray())
                }
                Toast.makeText(appContext, appContext.getString(R.string.reports_csv_saved), Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.reports_csv_failed, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                pendingCsvText.value = null
            }
        }
    )

    fun saveCsv(fileName: String, csv: String) {
        pendingCsvText.value = csv
        createCsvLauncher.launch(fileName)
    }

    val pendingPdfBytes = remember { mutableStateOf<ByteArray?>(null) }
    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val bytes = pendingPdfBytes.value ?: return@rememberLauncherForActivityResult
            try {
                appContext.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }
                Toast.makeText(appContext, appContext.getString(R.string.reports_pdf_saved), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.reports_pdf_failed, e.message ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                pendingPdfBytes.value = null
            }
        }
    )

    fun savePdf(fileName: String, bytes: ByteArray) {
        pendingPdfBytes.value = bytes
        createPdfLauncher.launch(fileName)
    }

    fun yearTxs(year: Int): List<TransactionItemUi> =
        state.transactions
            .asSequence()
            .filter { LocalDate.ofEpochDay(it.epochDay).year == year }
            .sortedBy { it.epochDay }
            .toList()

    fun rangeTxs(startEpochDay: Long, endEpochDay: Long): List<TransactionItemUi> =
        state.transactions
            .asSequence()
            .filter { it.epochDay in startEpochDay..endEpochDay }
            .sortedBy { it.epochDay }
            .toList()

    var showRangeDialog by rememberSaveable { mutableStateOf(false) }
    var pendingFormat by rememberSaveable { mutableStateOf(ExportFormat.CSV) }
    val rangePickerState = rememberDateRangePickerState()

    if (showRangeDialog) {
        RangePickerDialog(
            state = rangePickerState,
            onDismiss = { showRangeDialog = false },
            onConfirm = {
                val startMs = rangePickerState.selectedStartDateMillis
                val endMs = rangePickerState.selectedEndDateMillis
                if (startMs == null || endMs == null) {
                    showRangeDialog = false
                    return@RangePickerDialog
                }

                val startEpoch = epochDayFromMillis(startMs)
                val endEpoch = epochDayFromMillis(endMs)
                val txs = rangeTxs(startEpoch, endEpoch)

                val label = appContext.getString(
                    R.string.reports_custom_range_label,
                    Formatters.dateFromEpochDay(startEpoch),
                    Formatters.dateFromEpochDay(endEpoch)
                )

                if (!proEnabled) {
                    showRangeDialog = false
                    onOpenPaywall()
                    return@RangePickerDialog
                }

                when (pendingFormat) {
                    ExportFormat.CSV -> {
                        val csv = buildCsvDetailed(appContext, txs)
                        val fileName = appContext.getString(
                            R.string.reports_file_custom_csv,
                            appContext.getString(R.string.app_name),
                            startEpoch,
                            endEpoch
                        )
                        saveCsv(fileName, csv)
                    }

                    ExportFormat.PDF -> {
                        val income = txs.sumOf { if (it.type == TransactionType.INCOME) it.amountCents else 0L }
                        val expense = txs.sumOf { if (it.type == TransactionType.EXPENSE) it.amountCents else 0L }
                        val bytes = buildReportPdfBytesDetailed(
                            context = appContext,
                            title = appContext.getString(R.string.reports_export_title),
                            periodLabel = label,
                            txs = txs,
                            incomeCents = income,
                            expenseCents = expense
                        )
                        val fileName = appContext.getString(
                            R.string.reports_file_custom_pdf,
                            appContext.getString(R.string.app_name),
                            startEpoch,
                            endEpoch
                        )
                        savePdf(fileName, bytes)
                    }
                }

                showRangeDialog = false
            }
        )
    }

    fun doExport(format: ExportFormat, scope: ExportScope) {
        if (!proEnabled) {
            onOpenPaywall()
            return
        }

        when (scope) {
            ExportScope.MONTH -> {
                val txs = monthTxs
                when (format) {
                    ExportFormat.CSV -> {
                        val csv = buildCsvDetailed(appContext, txs)
                        val fileName = appContext.getString(
                            R.string.reports_file_month_csv,
                            appContext.getString(R.string.app_name),
                            month.year,
                            month.monthValue.toString().padStart(2, '0')
                        )
                        saveCsv(fileName, csv)
                    }

                    ExportFormat.PDF -> {
                        val bytes = buildReportPdfBytesDetailed(
                            context = appContext,
                            title = appContext.getString(R.string.reports_export_title),
                            periodLabel = "${month.month} ${month.year}",
                            txs = txs,
                            incomeCents = state.incomeCents,
                            expenseCents = state.expenseCents
                        )
                        val fileName = appContext.getString(
                            R.string.reports_file_month_pdf,
                            appContext.getString(R.string.app_name),
                            month.year,
                            month.monthValue.toString().padStart(2, '0')
                        )
                        savePdf(fileName, bytes)
                    }
                }
            }

            ExportScope.YEAR -> {
                val year = month.year
                val txs = yearTxs(year)
                when (format) {
                    ExportFormat.CSV -> {
                        val csv = buildCsvDetailed(appContext, txs)
                        val fileName = appContext.getString(
                            R.string.reports_file_year_csv,
                            appContext.getString(R.string.app_name),
                            year
                        )
                        saveCsv(fileName, csv)
                    }

                    ExportFormat.PDF -> {
                        val income = txs.sumOf { if (it.type == TransactionType.INCOME) it.amountCents else 0L }
                        val expense = txs.sumOf { if (it.type == TransactionType.EXPENSE) it.amountCents else 0L }
                        val bytes = buildReportPdfBytesDetailed(
                            context = appContext,
                            title = appContext.getString(R.string.reports_export_title),
                            periodLabel = appContext.getString(R.string.reports_year_label, year),
                            txs = txs,
                            incomeCents = income,
                            expenseCents = expense
                        )
                        val fileName = appContext.getString(
                            R.string.reports_file_year_pdf,
                            appContext.getString(R.string.app_name),
                            year
                        )
                        savePdf(fileName, bytes)
                    }
                }
            }

            ExportScope.CUSTOM -> {
                pendingFormat = format
                showRangeDialog = true
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_reports), fontWeight = FontWeight.SemiBold, color = textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item {
                    ExportCardPretty(
                        proEnabled = proEnabled,
                        onLockedClick = onOpenPaywall,
                        onCsvMonth = { doExport(ExportFormat.CSV, ExportScope.MONTH) },
                        onCsvYear = { doExport(ExportFormat.CSV, ExportScope.YEAR) },
                        onCsvCustom = { doExport(ExportFormat.CSV, ExportScope.CUSTOM) },
                        onPdfMonth = { doExport(ExportFormat.PDF, ExportScope.MONTH) },
                        onPdfYear = { doExport(ExportFormat.PDF, ExportScope.YEAR) },
                        onPdfCustom = { doExport(ExportFormat.PDF, ExportScope.CUSTOM) }
                    )
                }

                item {
                    MonthSwitcherCard(
                        month = month,
                        onPrev = vm::previousMonth,
                        onNext = vm::nextMonth,
                        accent = accent,
                        border = border,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                item { SectionHeader(stringResource(R.string.reports_monthly_overview)) }

                item {
                    SoftCard(border = border) {
                        if (state.isLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = accent, strokeWidth = 3.dp)
                            }
                        } else {
                            val incomeStr = remember(state.incomeCents) { Formatters.money(state.incomeCents) }
                            val expenseStr = remember(state.expenseCents) { Formatters.money(state.expenseCents) }
                            val balanceStr = remember(state.balanceCents) { Formatters.money(state.balanceCents) }

                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OverviewRow(
                                    label = stringResource(R.string.label_income),
                                    value = incomeStr,
                                    valueColor = incomeColor,
                                    labelColor = textSecondary
                                )
                                OverviewRow(
                                    label = stringResource(R.string.label_expenses),
                                    value = expenseStr,
                                    valueColor = expenseColor,
                                    labelColor = textSecondary
                                )
                                Divider(color = border)
                                OverviewRow(
                                    label = stringResource(R.string.label_balance),
                                    value = balanceStr,
                                    valueColor = if (state.balanceCents < 0) expenseColor else textPrimary,
                                    labelColor = textSecondary
                                )
                            }
                        }
                    }
                }

                item { SectionHeader(stringResource(R.string.reports_spending_by_category)) }

                item {
                    SoftCard(border = border) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            RangeTabs(selected = tab, onSelected = { tab = it })

                            if (byCategory.isEmpty()) {
                                Text(stringResource(R.string.reports_no_categories_this_month), color = textSecondary)
                            } else {
                                DonutChart(
                                    slices = byCategory,
                                    totalCents = state.expenseCents,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp),
                                    accent = accent,
                                    holeColor = cs.background,
                                    labelColor = textPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                                CategoryLegendMoney(slices = byCategory)
                            }
                        }
                    }
                }

                item {
                    SoftCard(border = border) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.reports_weekly_title), fontWeight = FontWeight.SemiBold, color = textPrimary)

                            if (byCategory.isEmpty()) {
                                Text(stringResource(R.string.reports_no_data_chart), color = textSecondary)
                            } else {
                                RoundedBarChart(
                                    bars = byCategory,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    accent = accent,
                                    grid = border,
                                    labelColor = textSecondary
                                )
                            }
                        }
                    }
                }

                item { SectionHeader(stringResource(R.string.reports_daily_net_trend)) }

                item {
                    SoftCard(border = border) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.reports_line_graph_title), fontWeight = FontWeight.SemiBold, color = textPrimary)

                            if (dailyNet.isEmpty()) {
                                Text(stringResource(R.string.reports_no_daily_data), color = textSecondary)
                            } else {
                                LineChart(
                                    points = dailyNet,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    accent = accent,
                                    grid = border,
                                    bgFill = cs.background
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ========================= EXPORT (CSV) ========================= */

private fun buildCsvDetailed(context: Context, transactions: List<TransactionItemUi>): String {
    fun esc(value: String): String {
        val v = value.replace("\"", "\"\"")
        return "\"$v\""
    }

    val sb = StringBuilder()
    sb.append(context.getString(R.string.reports_csv_header))
    sb.append("\n")

    transactions.forEach { tx ->
        val dateObj = LocalDate.ofEpochDay(tx.epochDay)
        val dateIso = dateObj.toString()              // 2026-01-12
        val year = dateObj.year.toString()            // 2026
        val category = tx.categoryName
        val type = when (tx.type) {
            TransactionType.INCOME -> context.getString(R.string.label_income)
            TransactionType.EXPENSE -> context.getString(R.string.label_expense)
        }
        val desc = tx.note
        val amount = amountDollarsSigned(tx.type, tx.amountCents) // ✅ income +, expense -

        sb.append(
            listOf(
                esc(dateIso),
                esc(year),
                esc(category),
                esc(type),
                esc(desc),
                esc(amount)
            ).joinToString(",")
        )
        sb.append("\n")
    }

    return sb.toString()
}

/* ========================= EXPORT (PDF) ========================= */

private fun buildReportPdfBytesDetailed(
    context: Context,
    title: String,
    periodLabel: String,
    txs: List<TransactionItemUi>,
    incomeCents: Long,
    expenseCents: Long
): ByteArray {
    val doc = PdfDocument()

    // A4-ish
    val pageW = 595
    val pageH = 842
    val margin = 36
    val line = 16

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 18f
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 12f
    }
    val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12f
    }
    val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 10.5f
    }

    fun drawHeaderRow(canvas: android.graphics.Canvas, y: Int) {
        canvas.drawText(context.getString(R.string.label_date), (margin + 0).toFloat(), y.toFloat(), boldPaint)
        canvas.drawText(context.getString(R.string.label_year), (margin + 80).toFloat(), y.toFloat(), boldPaint)
        canvas.drawText(context.getString(R.string.label_category), (margin + 130).toFloat(), y.toFloat(), boldPaint)
        canvas.drawText(context.getString(R.string.label_type), (margin + 260).toFloat(), y.toFloat(), boldPaint)
        canvas.drawText(context.getString(R.string.label_description), (margin + 320).toFloat(), y.toFloat(), boldPaint)
        canvas.drawText(context.getString(R.string.label_amount_dollars), (margin + 520).toFloat(), y.toFloat(), boldPaint)
    }

    val incomeText = Formatters.money(incomeCents)
    val expenseText = Formatters.money(expenseCents)
    val balanceText = Formatters.money(incomeCents - expenseCents)

    var pageNumber = 1
    var rowIndex = 0

    fun startPage(): PdfDocument.Page {
        val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create()
        return doc.startPage(info)
    }

    while (true) {
        val page = startPage()
        val canvas = page.canvas

        var y = margin

        if (pageNumber == 1) {
            canvas.drawText(title, margin.toFloat(), y.toFloat(), titlePaint)
            y += 26
            canvas.drawText(periodLabel, margin.toFloat(), y.toFloat(), subPaint)
            y += 18

            canvas.drawText(
                context.getString(R.string.reports_income_value, incomeText),
                margin.toFloat(),
                y.toFloat(),
                subPaint
            )
            y += 16
            canvas.drawText(
                context.getString(R.string.reports_expenses_value, expenseText),
                margin.toFloat(),
                y.toFloat(),
                subPaint
            )
            y += 16
            canvas.drawText(
                context.getString(R.string.reports_balance_value, balanceText),
                margin.toFloat(),
                y.toFloat(),
                boldPaint
            )
            y += 22
        } else {
            canvas.drawText(
                context.getString(R.string.reports_title_with_period, title, periodLabel),
                margin.toFloat(),
                y.toFloat(),
                subPaint
            )
            y += 22
        }

        // table header
        drawHeaderRow(canvas, y)
        y += 14

        val maxY = pageH - margin

        var wroteAny = false
        while (rowIndex < txs.size && y + line <= maxY) {
            val tx = txs[rowIndex]
            val d = LocalDate.ofEpochDay(tx.epochDay)
            val dateIso = d.toString()
            val year = d.year.toString()
            val category = tx.categoryName.take(16)
            val type = if (tx.type == TransactionType.INCOME) {
                context.getString(R.string.label_income)
            } else {
                context.getString(R.string.label_expense)
            }
            val desc = tx.note.replace("\n", " ").replace("\r", " ").take(28)
            val amount = amountDollarsSigned(tx.type, tx.amountCents)

            canvas.drawText(dateIso, (margin + 0).toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(year, (margin + 80).toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(category, (margin + 130).toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(type, (margin + 260).toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(desc, (margin + 320).toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(amount, (margin + 520).toFloat(), y.toFloat(), cellPaint)

            y += line
            rowIndex++
            wroteAny = true
        }

        doc.finishPage(page)

        if (rowIndex >= txs.size) break
        if (!wroteAny) break

        pageNumber++
    }

    val baos = ByteArrayOutputStream()
    doc.writeTo(baos)
    doc.close()
    return baos.toByteArray()
}

/* ========================= MONEY HELPERS ========================= */

private fun amountDollarsSigned(type: TransactionType, amountCents: Long): String {
    // We output numbers like: 300.00 or -45.75 (no currency symbol)
    val abs = kotlin.math.abs(amountCents)
    val major = abs / 100
    val minor = abs % 100
    val base = "$major.${minor.toString().padStart(2, '0')}"
    return if (type == TransactionType.EXPENSE) "-$base" else base
}

/* ========================= DATA HELPERS ========================= */

private fun buildDailyNetPoints(
    month: YearMonth,
    txs: List<TransactionItemUi>
): List<DailyPoint> {
    val days = month.lengthOfMonth()
    if (days <= 0) return emptyList()

    val startDate = month.atDay(1)
    val startEpoch = startDate.toEpochDay()

    val netByDay = HashMap<Long, Long>(days * 2)
    for (tx in txs) {
        val signed = when (tx.type) {
            TransactionType.INCOME -> tx.amountCents
            TransactionType.EXPENSE -> -tx.amountCents
        }
        netByDay[tx.epochDay] = (netByDay[tx.epochDay] ?: 0L) + signed
    }

    return (0 until days).map { d ->
        val date = startDate.plusDays(d.toLong())
        val epoch = startEpoch + d
        DailyPoint(date, netByDay[epoch] ?: 0L)
    }
}

private fun epochDayFromMillis(millis: Long): Long {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return date.toEpochDay()
}

/* ========================= UI HELPERS ========================= */

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SoftCard(
    border: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, border)
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun MonthSwitcherCard(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    accent: Color,
    border: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    SoftCard(border = border) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPrev) { Text(stringResource(R.string.action_prev), color = accent) }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${month.month} ${month.year}", fontWeight = FontWeight.Bold, color = textPrimary)
                Text(stringResource(R.string.reports_monthly_report), color = textSecondary, style = MaterialTheme.typography.bodySmall)
            }

            TextButton(onClick = onNext) { Text(stringResource(R.string.action_next), color = accent) }
        }
    }
}

@Composable
private fun OverviewRow(
    label: String,
    value: String,
    valueColor: Color,
    labelColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = labelColor)
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

/* ========================= RANGE TABS ========================= */

@Composable
private fun RangeTabs(
    selected: RangeTab,
    onSelected: (RangeTab) -> Unit
) {
    val container = MaterialTheme.colorScheme.surfaceVariant
    val selectedBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(color = container, shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            @Composable
            fun Pill(tab: RangeTab, label: String) {
                val isSel = selected == tab
                Surface(
                    onClick = { onSelected(tab) },
                    color = if (isSel) selectedBg else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (isSel) textPrimary else textSecondary,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }

            Pill(RangeTab.TODAY, stringResource(R.string.reports_range_today))
            Pill(RangeTab.WEEK, stringResource(R.string.reports_range_week))
            Pill(RangeTab.MONTH, stringResource(R.string.reports_range_month))
            Pill(RangeTab.CUSTOM, stringResource(R.string.reports_range_custom))
        }
    }
}

/* ========================= LEGEND ========================= */

@Composable
private fun CategoryLegendMoney(slices: List<CategorySlice>) {
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        slices.forEach { s ->
            val money = remember(s.valueCents) { Formatters.money(s.valueCents) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.label, color = textSecondary, fontWeight = FontWeight.Medium)
                Text(money, color = textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/* ========================= DONUT CHART ========================= */

@Composable
private fun DonutChart(
    slices: List<CategorySlice>,
    totalCents: Long,
    modifier: Modifier = Modifier,
    accent: Color,
    holeColor: Color,
    labelColor: Color
) {
    val palette = remember(accent) {
        listOf(
            accent,
            Color(0xFF6BC7C5),
            Color(0xFF5B8EF0),
            Color(0xFFE38B8B),
            Color(0xFFB39DDB)
        )
    }

    val total = remember(slices) { slices.sumOf { it.valueCents }.coerceAtLeast(1L) }
    val centerLabel = remember(totalCents) { Formatters.money(totalCents) }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = min(size.width, size.height)
            val stroke = (d * 0.18f).coerceIn(18f, 52f)
            val pad = stroke / 2f + 10f

            labelPaint.textSize = (stroke * 0.45f).coerceIn(18f, 28f)

            val rect = Rect(
                left = (size.width - d) / 2f + pad,
                top = (size.height - d) / 2f + pad,
                right = (size.width + d) / 2f - pad,
                bottom = (size.height + d) / 2f - pad
            )

            val gapDegrees = 4f
            var startAngle = -90f

            slices.forEachIndexed { idx, s ->
                val rawSweep = 360f * (s.valueCents.toFloat() / total.toFloat())
                val sweep = (rawSweep - gapDegrees).coerceAtLeast(1f)

                drawArc(
                    color = palette[idx % palette.size].copy(alpha = 0.95f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                val mid = (startAngle + sweep / 2f) * (Math.PI / 180.0)
                val r = rect.width / 2f
                val labelRadius = r * 0.78f
                val cx = rect.center.x + (kotlin.math.cos(mid) * labelRadius).toFloat()
                val cy = rect.center.y + (kotlin.math.sin(mid) * labelRadius).toFloat()

                val sliceMoney = Formatters.money(s.valueCents)
                drawContext.canvas.nativeCanvas.drawText(
                    sliceMoney,
                    cx,
                    cy + labelPaint.textSize / 3f,
                    labelPaint
                )

                startAngle += rawSweep
            }

            drawCircle(
                color = holeColor,
                radius = (rect.width / 2f) - stroke * 0.75f,
                center = rect.center
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

/* ========================= BAR CHART ========================= */

@Composable
private fun RoundedBarChart(
    bars: List<CategorySlice>,
    modifier: Modifier = Modifier,
    accent: Color,
    grid: Color,
    labelColor: Color
) {
    if (bars.isEmpty()) return

    val max = remember(bars) { (bars.maxOfOrNull { it.valueCents } ?: 1L).coerceAtLeast(1L) }

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(210, 160, 160, 160)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val leftPad = 10f
        val rightPad = 10f
        val topPad = 14f
        val bottomPad = 26f

        val chartW = (w - leftPad - rightPad).coerceAtLeast(1f)
        val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)

        repeat(3) { i ->
            val y = topPad + chartH * ((i + 1) / 4f)
            drawLine(
                color = grid.copy(alpha = 0.55f),
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 2f
            )
        }

        drawLine(
            color = grid,
            start = Offset(leftPad, topPad + chartH),
            end = Offset(leftPad + chartW, topPad + chartH),
            strokeWidth = 2f
        )

        val gap = 12f
        val barW = ((chartW - gap * (bars.size - 1)) / bars.size).coerceAtLeast(10f)

        bars.forEachIndexed { idx, b ->
            val x = leftPad + idx * (barW + gap)

            val ratio = (b.valueCents.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            val barH = chartH * ratio
            val y = topPad + (chartH - barH)

            drawRoundRect(
                color = accent.copy(alpha = 0.20f),
                topLeft = Offset(x - 2f, y - 2f),
                size = Size(barW + 4f, barH + 4f),
                cornerRadius = CornerRadius(18f, 18f)
            )

            drawRoundRect(
                color = accent.copy(alpha = 0.85f),
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(18f, 18f)
            )

            val label = Formatters.money(b.valueCents)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barW / 2f,
                (y - 8f).coerceAtLeast(18f),
                labelPaint
            )
        }
    }
}

/* ========================= LINE CHART ========================= */

@Composable
private fun LineChart(
    points: List<DailyPoint>,
    modifier: Modifier = Modifier,
    accent: Color,
    grid: Color,
    bgFill: Color
) {
    if (points.size < 2) return

    val values = remember(points) { points.map { it.netCents.toFloat() } }
    val minV = remember(values) { values.minOrNull() ?: 0f }
    val maxV = remember(values) { values.maxOrNull() ?: 1f }
    val range = remember(minV, maxV) { (maxV - minV).takeIf { it != 0f } ?: 1f }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val leftPad = 10f
        val rightPad = 10f
        val topPad = 18f
        val bottomPad = 18f

        val chartW = (w - leftPad - rightPad).coerceAtLeast(1f)
        val chartH = (h - topPad - bottomPad).coerceAtLeast(1f)

        repeat(4) { i ->
            val y = topPad + chartH * (i / 3f)
            drawLine(
                color = grid.copy(alpha = 0.55f),
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 2f
            )
        }

        fun yFor(v: Float): Float {
            val t = (v - minV) / range
            return topPad + (chartH - (t * chartH))
        }

        val stepX = chartW / (points.size - 1)
        val pts = points.mapIndexed { idx, p ->
            Offset(leftPad + idx * stepX, yFor(p.netCents.toFloat()))
        }

        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 0 until pts.size - 1) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val cX = (p0.x + p1.x) / 2f
                cubicTo(cX, p0.y, cX, p1.y, p1.x, p1.y)
            }
        }

        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(pts.last().x, topPad + chartH)
            lineTo(pts.first().x, topPad + chartH)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                listOf(
                    accent.copy(alpha = 0.22f),
                    bgFill.copy(alpha = 0.0f)
                )
            )
        )

        drawPath(
            path = linePath,
            color = accent.copy(alpha = 0.90f),
            style = Stroke(width = 7f, cap = StrokeCap.Round)
        )
    }
}

/* ========================= RANGE PICKER DIALOG ========================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangePickerDialog(
    state: DateRangePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_export)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        title = { Text(stringResource(R.string.reports_select_range_title)) },
        text = { DateRangePicker(state = state) }
    )
}

/* ========================= EXPORT UI ========================= */

@Composable
private fun ExportCardPretty(
    proEnabled: Boolean,
    onLockedClick: () -> Unit,
    onCsvMonth: () -> Unit,
    onCsvYear: () -> Unit,
    onCsvCustom: () -> Unit,
    onPdfMonth: () -> Unit,
    onPdfYear: () -> Unit,
    onPdfCustom: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val border = cs.outlineVariant
    val textPrimary = cs.onSurface
    val textSecondary = cs.onSurfaceVariant

    SoftCard(border = border) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.action_export),
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (proEnabled) {
                            stringResource(R.string.reports_export_subtitle)
                        } else {
                            stringResource(R.string.reports_export_locked_subtitle)
                        },
                        color = textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ProPill(
                    proEnabled = proEnabled,
                    onClick = { if (!proEnabled) onLockedClick() }
                )
            }

            ExportRow(
                title = stringResource(R.string.reports_export_csv_title),
                subtitle = stringResource(R.string.reports_export_csv_subtitle),
                iconText = stringResource(R.string.reports_export_csv_badge),
                proEnabled = proEnabled,
                onLockedClick = onLockedClick,
                onMonth = onCsvMonth,
                onYear = onCsvYear,
                onCustom = onCsvCustom
            )

            ExportRow(
                title = stringResource(R.string.reports_export_pdf_title),
                subtitle = stringResource(R.string.reports_export_pdf_subtitle),
                iconText = stringResource(R.string.reports_export_pdf_badge),
                proEnabled = proEnabled,
                onLockedClick = onLockedClick,
                onMonth = onPdfMonth,
                onYear = onPdfYear,
                onCustom = onPdfCustom
            )
        }
    }
}

@Composable
private fun ExportRow(
    title: String,
    subtitle: String,
    iconText: String,
    proEnabled: Boolean,
    onLockedClick: () -> Unit,
    onMonth: () -> Unit,
    onYear: () -> Unit,
    onCustom: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val border = cs.outlineVariant
    val textPrimary = cs.onSurface
    val textSecondary = cs.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cs.surface,
        border = BorderStroke(1.dp, border.copy(alpha = 0.9f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = { if (!proEnabled) onLockedClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExportBadge(text = iconText)

                Spacer(Modifier.width(10.dp))

                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    Text(subtitle, color = textSecondary, style = MaterialTheme.typography.bodySmall)
                }

                if (!proEnabled) LockedPill(onClick = onLockedClick)
            }

            FlowRowNice {
                ExportChip(
                    label = stringResource(R.string.reports_range_month),
                    enabled = proEnabled,
                    onLockedClick = onLockedClick,
                    onClick = onMonth
                )
                ExportChip(
                    label = stringResource(R.string.reports_range_year),
                    enabled = proEnabled,
                    onLockedClick = onLockedClick,
                    onClick = onYear
                )
                ExportChip(
                    label = stringResource(R.string.reports_range_custom),
                    enabled = proEnabled,
                    onLockedClick = onLockedClick,
                    onClick = onCustom
                )
            }
        }
    }
}

@Composable
private fun ExportBadge(text: String) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cs.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            color = cs.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun ProPill(
    proEnabled: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (proEnabled) cs.primary.copy(alpha = 0.14f) else cs.surfaceVariant.copy(alpha = 0.55f)
    val fg = if (proEnabled) cs.primary else cs.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, cs.outlineVariant),
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.reports_pro_label),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = fg,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun LockedPill(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = cs.error.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, cs.outlineVariant),
        onClick = onClick
    ) {
        Text(
            text = stringResource(R.string.reports_locked_label),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = cs.error,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ExportChip(
    label: String,
    enabled: Boolean,
    onLockedClick: () -> Unit,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val accent = cs.primary

    Surface(
        onClick = { if (enabled) onClick() else onLockedClick() },
        enabled = true,
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) accent.copy(alpha = 0.12f) else cs.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, cs.outlineVariant)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (enabled) accent else cs.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun FlowRowNice(content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = Modifier.fillMaxWidth()
    ) { measurables, constraints ->
        val horizontalGap = 10.dp.roundToPx()
        val verticalGap = 10.dp.roundToPx()

        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

        var x = 0
        var y = 0
        var rowH = 0
        val maxW = constraints.maxWidth

        val positions = ArrayList<Pair<Int, Int>>(placeables.size)

        for (p in placeables) {
            val nextX = if (x == 0) p.width else x + horizontalGap + p.width
            if (nextX > maxW) {
                x = 0
                y += rowH + verticalGap
                rowH = 0
            }

            positions.add(x to y)
            x = if (x == 0) p.width else x + horizontalGap + p.width
            rowH = maxOf(rowH, p.height)
        }

        val height = (y + rowH).coerceAtLeast(0)

        layout(width = maxW, height = height) {
            placeables.forEachIndexed { i, p ->
                val (px, py) = positions[i]
                p.placeRelative(px, py)
            }
        }
    }
}
