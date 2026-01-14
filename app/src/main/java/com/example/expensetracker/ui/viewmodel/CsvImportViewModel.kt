package com.example.expensetracker.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.data.model.MerchantRuleEntity
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.util.BankCsvParser
import com.example.expensetracker.util.BankCsvRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs

data class CsvPreviewRow(
    val date: LocalDate,
    val description: String,
    val amountCentsAbs: Long,
    val typeUpper: String
)

data class CsvImportState(
    val loading: Boolean = false,
    val pickedName: String? = null,

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
    val importedCount: Int? = null
)

class CsvImportViewModel(
    private val context: Context,
    private val db: AppDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(CsvImportState(loading = true))
    val state: StateFlow<CsvImportState> = _state

    private var lastParsed: List<BankCsvRow> = emptyList()

    init {
        viewModelScope.launch { loadCategories() }
    }

    fun selectCategory(id: Long) {
        _state.value = _state.value.copy(selectedCategoryId = id, importedCount = null, error = null)
        if (lastParsed.isNotEmpty()) {
            viewModelScope.launch { recomputeDuplicatesAndTotals() }
        }
    }

    fun pickAndParse(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                pickedName = uri.lastPathSegment,
                error = null,
                importedCount = null,
                warnings = emptyList()
            )

            val result = try {
                withContext(Dispatchers.IO) { BankCsvParser.parseFromUri(context, uri) }
            } catch (t: Throwable) {
                _state.value = _state.value.copy(loading = false, error = "Failed to parse CSV: ${t.message}")
                return@launch
            }

            lastParsed = result.rows

            val preview = result.rows
                .sortedWith(compareBy<BankCsvRow> { it.date }.thenBy { it.rawLineIndex })
                .map { r ->
                    val typeUpper = if (r.signedAmountCents < 0L) "EXPENSE" else "INCOME"
                    CsvPreviewRow(
                        date = r.date,
                        description = r.description,
                        amountCentsAbs = abs(r.signedAmountCents),
                        typeUpper = typeUpper
                    )
                }

            val start = preview.firstOrNull()?.date
            val end = preview.lastOrNull()?.date

            _state.value = _state.value.copy(
                loading = false,
                rows = preview,
                start = start,
                end = end,
                warnings = result.warnings,
                error = null
            )

            recomputeDuplicatesAndTotals()
        }
    }

    fun importNow() {
        viewModelScope.launch {
            val s = _state.value
            val fallbackCategoryId = s.selectedCategoryId
            if (fallbackCategoryId == null) {
                _state.value = s.copy(error = "Pick a category first.")
                return@launch
            }
            if (lastParsed.isEmpty()) {
                _state.value = s.copy(error = "No rows parsed.")
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
                    val type = if (r.signedAmountCents < 0L) TransactionType.EXPENSE else TransactionType.INCOME
                    val amountAbs = abs(r.signedAmountCents)

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
                _state.value = _state.value.copy(loading = false, error = "Import failed: ${t.message}")
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
        val preview = _state.value.rows
        if (preview.isEmpty()) {
            _state.value = _state.value.copy(
                willImportCount = 0,
                duplicatesDetected = 0,
                previewIncomeCents = 0L,
                previewExpenseCents = 0L
            )
            return
        }

        val income = preview.sumOf { if (it.typeUpper == "INCOME") it.amountCentsAbs else 0L }
        val expense = preview.sumOf { if (it.typeUpper == "EXPENSE") it.amountCentsAbs else 0L }

        val existing = withContext(Dispatchers.IO) { buildExistingFingerprintsForParsed() }

        var dupes = 0
        for (r in preview) {
            val type = if (r.typeUpper == "EXPENSE") TransactionType.EXPENSE else TransactionType.INCOME
            val fp = fingerprint(r.date.toEpochDay(), type, abs(r.amountCentsAbs), r.description.trim())
            if (existing.contains(fp)) dupes++
        }

        _state.value = _state.value.copy(
            previewIncomeCents = income,
            previewExpenseCents = expense,
            duplicatesDetected = dupes,
            willImportCount = (preview.size - dupes).coerceAtLeast(0)
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
