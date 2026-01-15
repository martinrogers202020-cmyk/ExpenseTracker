package com.example.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.R
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import com.example.expensetracker.data.repo.CategoryRepository
import com.example.expensetracker.data.repo.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class AddTransactionViewModel(
    private val context: Context,
    private val categoryRepo: CategoryRepository,
    private val txRepo: TransactionRepository
) : ViewModel() {

    val categories = categoryRepo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun clearSaved() {
        _saved.value = false
    }

    fun saveIncome(
        amountText: String,
        dateText: String,
        categoryId: Long?,
        notes: String,
        attachmentUri: String?
    ) {
        save(
            type = TransactionType.INCOME,
            amountText = amountText,
            dateText = dateText,
            categoryId = categoryId,
            notes = notes,
            attachmentUri = attachmentUri
        )
    }

    fun saveExpense(
        amountText: String,
        dateText: String,
        categoryId: Long?,
        notes: String,
        attachmentUri: String?
    ) {
        save(
            type = TransactionType.EXPENSE,
            amountText = amountText,
            dateText = dateText,
            categoryId = categoryId,
            notes = notes,
            attachmentUri = attachmentUri
        )
    }

    private fun save(
        type: TransactionType,
        amountText: String,
        dateText: String,
        categoryId: Long?,
        notes: String,
        attachmentUri: String?
    ) {
        viewModelScope.launch {
            if (_saving.value) return@launch
            _error.value = null
            _saved.value = false

            val cents = dollarsTextToCents(amountText)
            if (cents == null || cents <= 0L) {
                _error.value = context.getString(R.string.transaction_error_invalid_amount)
                return@launch
            }

            val epochDay = try {
                LocalDate.parse(dateText).toEpochDay()
            } catch (_: Throwable) {
                _error.value = context.getString(R.string.transaction_error_invalid_date)
                return@launch
            }

            val catId = categoryId
            if (catId == null) {
                _error.value = context.getString(R.string.transaction_error_pick_category)
                return@launch
            }

            val finalNote = buildString {
                val n = notes.trim()
                if (n.isNotEmpty()) append(n)

                val att = attachmentUri?.trim().orEmpty()
                if (att.isNotEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append(context.getString(R.string.transaction_attachment_prefix, att))
                }
            }

            _saving.value = true
            try {
                txRepo.insert(
                    TransactionEntity(
                        id = 0L,
                        type = type,
                        amountCents = cents, // ✅ ALWAYS store cents
                        categoryId = catId,
                        note = finalNote,
                        epochDay = epochDay
                    )
                )
                _saved.value = true
            } catch (t: Throwable) {
                _error.value = t.message ?: context.getString(R.string.transaction_error_save_failed)
            } finally {
                _saving.value = false
            }
        }
    }

    /**
     * Converts "100" -> 10000, "100.5" -> 10050, "100.50" -> 10050
     * Accepts commas and $ signs.
     * Uses BigDecimal to avoid floating point errors.
     */
    private fun dollarsTextToCents(input: String): Long? {
        val raw = input.trim()
        if (raw.isEmpty()) return null

        val cleaned = raw
            .replace("$", "")
            .replace(",", "")
            .trim()

        // Reject negative here (type decides INCOME vs EXPENSE)
        if (cleaned.startsWith("-")) return null

        return try {
            val bd = BigDecimal(cleaned)
                .setScale(2, RoundingMode.HALF_UP)

            bd.movePointRight(2).longValueExact()
        } catch (_: Throwable) {
            null
        }
    }
}
