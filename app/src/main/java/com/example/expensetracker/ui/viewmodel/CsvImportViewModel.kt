package com.example.expensetracker.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.R
import com.example.expensetracker.data.datastore.ImportMappingStore
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.importer.ColumnMapping
import com.example.expensetracker.data.importer.CsvImporter
import com.example.expensetracker.data.importer.DecimalSeparator
import com.example.expensetracker.data.importer.FormatDetection
import com.example.expensetracker.data.importer.FormatDetector
import com.example.expensetracker.data.importer.ImportFormat
import com.example.expensetracker.data.importer.ImportSource
import com.example.expensetracker.data.importer.ImportedTxn
import com.example.expensetracker.data.importer.Importer
import com.example.expensetracker.data.importer.ImporterResult
import com.example.expensetracker.data.importer.ImportParsingUtils
import com.example.expensetracker.data.importer.OfxImporter
import com.example.expensetracker.data.importer.PdfTextImporter
import com.example.expensetracker.data.importer.QifImporter
import com.example.expensetracker.data.importer.XlsxImporter
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.data.model.MerchantRuleEntity
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.abs

@Immutable
data class CsvPreviewRow(
    val date: LocalDate,
    val description: String,
    val amountCentsAbs: Long,
    val typeUpper: String
)

@Immutable
data class CsvImportState(
    val loading: Boolean = false,
    val pickedName: String? = null,
    val detectedFormatLabel: String? = null,

    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: Long? = null,

    val rows: List<CsvPreviewRow> = emptyList(),
    val start: LocalDate? = null,
    val end: LocalDate? = null,

    val previewIncomeCents: Long = 0L,
    val previewExpenseCents: Long = 0L,

    val willImportCount: Int = 0,
    val duplicatesDetected: Int = 0,

    val warnings: List<String> = emptyList(),
    val error: String? = null,
    val importedCount: Int? = null,

    val mappingRequired: Boolean = false,
    val mappingColumns: List<String> = emptyList(),
    val mappingSampleRows: List<List<String>> = emptyList(),
    val mappingDateColumn: Int? = null,
    val mappingDescriptionColumn: Int? = null,
    val mappingAmountColumn: Int? = null,
    val mappingDebitColumn: Int? = null,
    val mappingCreditColumn: Int? = null,
    val mappingDateFormat: String? = null,
    val mappingDecimalSeparator: DecimalSeparator = DecimalSeparator.DOT,
    val mappingProfileKey: String? = null,
    val mappingError: String? = null
)

class CsvImportViewModel(
    private val context: Context,
    private val db: AppDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(CsvImportState(loading = true))
    val state: StateFlow<CsvImportState> = _state

    private var lastParsed: List<ImportedTxn> = emptyList()
    private var lastImporter: Importer? = null
    private var lastSource: ImportSource? = null

    init {
        viewModelScope.launch { loadCategories() }
    }

    fun selectCategory(id: Long) {
        _state.value = _state.value.copy(selectedCategoryId = id, importedCount = null, error = null)
        if (lastParsed.isNotEmpty()) {
            viewModelScope.launch { recomputeDuplicatesAndTotals() }
        }
    }

    fun updateMappingDateColumn(index: Int?) {
        _state.value = _state.value.copy(mappingDateColumn = index, mappingError = null)
    }

    fun updateMappingDescriptionColumn(index: Int?) {
        _state.value = _state.value.copy(mappingDescriptionColumn = index, mappingError = null)
    }

    fun updateMappingAmountColumn(index: Int?) {
        _state.value = _state.value.copy(mappingAmountColumn = index, mappingError = null)
    }

    fun updateMappingDebitColumn(index: Int?) {
        _state.value = _state.value.copy(mappingDebitColumn = index, mappingError = null)
    }

    fun updateMappingCreditColumn(index: Int?) {
        _state.value = _state.value.copy(mappingCreditColumn = index, mappingError = null)
    }

    fun updateMappingDateFormat(format: String?) {
        _state.value = _state.value.copy(mappingDateFormat = format, mappingError = null)
    }

    fun updateMappingDecimalSeparator(separator: DecimalSeparator) {
        _state.value = _state.value.copy(mappingDecimalSeparator = separator, mappingError = null)
    }

    fun pickAndParse(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                pickedName = uri.lastPathSegment,
                error = null,
                importedCount = null,
                warnings = emptyList(),
                mappingRequired = false,
                mappingError = null
            )
            val source = ImportSource(uri = uri, displayName = uri.lastPathSegment)
            val detection = FormatDetector.detect(context, uri, source.displayName)
            val importer = importerFor(detection.format)
            lastImporter = importer
            lastSource = source

            val result = try {
                withContext(Dispatchers.IO) { importer.import(context, source, null) }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = context.getString(R.string.csv_import_failed_parse, t.message ?: "")
                )
                return@launch
            }

            handleImportResult(detection, result)
        }
    }

    fun applyMapping() {
        viewModelScope.launch {
            val s = _state.value
            val importer = lastImporter ?: return@launch
            val source = lastSource ?: return@launch

            val mapping = buildMappingFromState(s) ?: run {
                _state.value = s.copy(mappingError = context.getString(R.string.import_mapping_incomplete))
                return@launch
            }

            _state.value = s.copy(loading = true, mappingError = null)

            val result = try {
                withContext(Dispatchers.IO) { importer.import(context, source, mapping) }
            } catch (t: Throwable) {
                _state.value = s.copy(
                    loading = false,
                    error = context.getString(R.string.csv_import_failed_parse, t.message ?: "")
                )
                return@launch
            }

            val profileKey = result.diagnostics.bankProfileKey ?: s.mappingProfileKey
            if (!profileKey.isNullOrBlank()) {
                withContext(Dispatchers.IO) { ImportMappingStore.saveMapping(context, profileKey, mapping) }
            }

            handleImportResult(FormatDetector.detect(context, source.uri, source.displayName), result)
        }
    }

    fun importNow() {
        viewModelScope.launch {
            val s = _state.value
            val fallbackCategoryId = s.selectedCategoryId
            if (fallbackCategoryId == null) {
                _state.value = s.copy(error = context.getString(R.string.csv_import_pick_category_error))
                return@launch
            }
            if (lastParsed.isEmpty()) {
                _state.value = s.copy(error = context.getString(R.string.csv_import_no_rows_error))
                return@launch
            }

            _state.value = s.copy(loading = true, error = null, importedCount = null)

            val rules = try {
                withContext(Dispatchers.IO) { db.merchantRuleDao().getAllOnce().filter { it.enabled } }
            } catch (_: Throwable) {
                emptyList()
            }

            val (toInsert, dupes) = withContext(Dispatchers.IO) {
                val existing = buildExistingFingerprintsForParsed()
                val inserts = ArrayList<TransactionEntity>()
                var d = 0

                for (r in lastParsed) {
                    val type = if (r.amount < BigDecimal.ZERO) TransactionType.EXPENSE else TransactionType.INCOME
                    val amountAbs = abs(ImportParsingUtils.normalizeAmountToCents(r.amount))

                    val catId = bestCategoryForDescription(r.description, rules) ?: fallbackCategoryId

                    val note = r.description.trim()
                    val epochDay = r.date.toEpochDay()

                    val fp = fingerprint(epochDay, type, amountAbs, note)
                    if (existing.contains(fp)) {
                        d++
                        continue
                    }

                    inserts.add(
                        TransactionEntity(
                            id = 0L,
                            type = type,
                            amountCents = amountAbs,
                            categoryId = catId,
                            note = note,
                            epochDay = epochDay
                        )
                    )
                }

                Pair(inserts, d)
            }

            try {
                withContext(Dispatchers.IO) {
                    if (toInsert.isNotEmpty()) {
                        db.transactionDao().insertAll(toInsert)
                    }
                }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = context.getString(R.string.csv_import_failed_import, t.message ?: "")
                )
                return@launch
            }

            _state.value = _state.value.copy(
                loading = false,
                importedCount = toInsert.size,
                duplicatesDetected = dupes
            )

            recomputeDuplicatesAndTotals()
        }
    }

    /* ---------------- helpers ---------------- */

    private fun importerFor(format: ImportFormat): Importer {
        return when (format) {
            ImportFormat.CSV -> CsvImporter()
            ImportFormat.XLSX -> XlsxImporter()
            ImportFormat.OFX -> OfxImporter()
            ImportFormat.QIF -> QifImporter()
            ImportFormat.PDF -> PdfTextImporter()
        }
    }

    private suspend fun handleImportResult(
        detection: FormatDetection,
        initialResult: ImporterResult
    ) {
        var result = initialResult
        val diagnostics = result.diagnostics
        val importer = lastImporter
        val source = lastSource
        val profileKey = diagnostics.bankProfileKey

        if (diagnostics.needsMapping && !profileKey.isNullOrBlank() && importer != null && source != null) {
            val stored = ImportMappingStore.loadMapping(context, profileKey)
            if (stored != null) {
                result = withContext(Dispatchers.IO) { importer.import(context, source, stored) }
            }
        }

        val finalDiagnostics = result.diagnostics
        lastParsed = result.transactions

        val preview = lastParsed
            .sortedWith(compareBy<ImportedTxn> { it.date }.thenBy { it.description })
            .map { r ->
                val amountAbs = abs(ImportParsingUtils.normalizeAmountToCents(r.amount))
                CsvPreviewRow(
                    date = r.date,
                    description = r.description,
                    amountCentsAbs = amountAbs,
                    typeUpper = if (r.amount < BigDecimal.ZERO) "EXPENSE" else "INCOME"
                )
            }

        val start = preview.firstOrNull()?.date
        val end = preview.lastOrNull()?.date

        val suggested = finalDiagnostics.suggestedMapping
        _state.value = _state.value.copy(
            loading = false,
            rows = preview,
            start = start,
            end = end,
            warnings = finalDiagnostics.warnings,
            error = null,
            detectedFormatLabel = detection.format.name,
            mappingRequired = finalDiagnostics.needsMapping,
            mappingColumns = finalDiagnostics.columns,
            mappingSampleRows = finalDiagnostics.sampleRows,
            mappingDateColumn = suggested?.dateColumn,
            mappingDescriptionColumn = suggested?.descriptionColumn,
            mappingAmountColumn = suggested?.amountColumn,
            mappingDebitColumn = suggested?.debitColumn,
            mappingCreditColumn = suggested?.creditColumn,
            mappingDateFormat = suggested?.dateFormat,
            mappingDecimalSeparator = suggested?.decimalSeparator ?: DecimalSeparator.DOT,
            mappingProfileKey = finalDiagnostics.bankProfileKey,
            mappingError = null
        )

        recomputeDuplicatesAndTotals()
    }

    private fun buildMappingFromState(state: CsvImportState): ColumnMapping? {
        val date = state.mappingDateColumn ?: return null
        val hasAmount = state.mappingAmountColumn != null
        val hasDebitCredit = state.mappingDebitColumn != null || state.mappingCreditColumn != null
        if (!hasAmount && !hasDebitCredit) return null

        return ColumnMapping(
            dateColumn = date,
            descriptionColumn = state.mappingDescriptionColumn,
            typeColumn = null,
            amountColumn = state.mappingAmountColumn,
            debitColumn = state.mappingDebitColumn,
            creditColumn = state.mappingCreditColumn,
            dateFormat = state.mappingDateFormat,
            decimalSeparator = state.mappingDecimalSeparator
        )
    }

    private suspend fun loadCategories() {
        val cats = try {
            withContext(Dispatchers.IO) { db.categoryDao().getAllOnce() }
        } catch (_: Throwable) {
            emptyList()
        }

        val defaultId = cats.firstOrNull { it.isDefault }?.id ?: cats.firstOrNull()?.id
        _state.value = _state.value.copy(
            loading = false,
            categories = cats,
            selectedCategoryId = _state.value.selectedCategoryId ?: defaultId
        )
    }

    private suspend fun recomputeDuplicatesAndTotals() {
        if (lastParsed.isEmpty()) {
            _state.value = _state.value.copy(
                willImportCount = 0,
                duplicatesDetected = 0,
                previewIncomeCents = 0L,
                previewExpenseCents = 0L
            )
            return
        }

        val income = lastParsed.sumOf { if (it.amount >= BigDecimal.ZERO) abs(ImportParsingUtils.normalizeAmountToCents(it.amount)) else 0L }
        val expense = lastParsed.sumOf { if (it.amount < BigDecimal.ZERO) abs(ImportParsingUtils.normalizeAmountToCents(it.amount)) else 0L }

        val existing = withContext(Dispatchers.IO) { buildExistingFingerprintsForParsed() }

        var dupes = 0
        for (r in lastParsed) {
            val type = if (r.amount < BigDecimal.ZERO) TransactionType.EXPENSE else TransactionType.INCOME
            val amountAbs = abs(ImportParsingUtils.normalizeAmountToCents(r.amount))
            val fp = fingerprint(r.date.toEpochDay(), type, amountAbs, r.description.trim())
            if (existing.contains(fp)) dupes++
        }

        _state.value = _state.value.copy(
            previewIncomeCents = income,
            previewExpenseCents = expense,
            duplicatesDetected = dupes,
            willImportCount = (lastParsed.size - dupes).coerceAtLeast(0)
        )
    }

    private suspend fun buildExistingFingerprintsForParsed(): HashSet<String> {
        val parsed = lastParsed
        if (parsed.isEmpty()) return hashSetOf()

        val minDay = parsed.minOf { it.date.toEpochDay() }
        val maxDay = parsed.maxOf { it.date.toEpochDay() }

        val existingTxs = try {
            db.transactionDao().getBetween(minDay, maxDay)
        } catch (_: Throwable) {
            emptyList()
        }

        val set = HashSet<String>(existingTxs.size * 2)
        for (tx in existingTxs) {
            val note = tx.note.orEmpty()
            set.add(fingerprint(tx.epochDay, tx.type, abs(tx.amountCents), note))
        }
        return set
    }

    private fun bestCategoryForDescription(
        description: String,
        rules: List<MerchantRuleEntity>
    ): Long? {
        val desc = description.trim()
        if (desc.isBlank()) return null

        val ordered = rules
            .filter { it.enabled }
            .sortedWith(compareByDescending<MerchantRuleEntity> { it.priority }.thenByDescending { it.createdAtEpochMs })

        for (r in ordered) {
            val p = r.pattern.trim()
            if (p.isBlank()) continue

            val ok = when (r.matchType.uppercase()) {
                "STARTS_WITH" -> desc.startsWith(p, ignoreCase = true)
                "REGEX" -> runCatching { Regex(p, RegexOption.IGNORE_CASE).containsMatchIn(desc) }.getOrElse { false }
                else -> desc.contains(p, ignoreCase = true) // CONTAINS default
            }

            if (ok) return r.categoryId
        }
        return null
    }

    private fun fingerprint(epochDay: Long, type: TransactionType, amountAbs: Long, note: String): String {
        val n = note.trim().lowercase()
            .replace("\n", " ")
            .replace("\r", " ")
            .replace(Regex("\\s+"), " ")
        return "$epochDay|${type.name}|$amountAbs|$n"
    }
}
