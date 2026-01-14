package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.CategoryDao
import com.example.expensetracker.data.db.TransactionDao
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.data.model.TransactionEntity
import com.example.expensetracker.data.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs

data class CategoryOptionUi(
    val id: Long,
    val label: String
)

data class AddEditUiState(
    val isEdit: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val categoryId: Long? = null,
    val categories: List<CategoryOptionUi> = emptyList(),
    val note: String = "",
    val error: String? = null,
    val isSaving: Boolean = false
)

class AddEditViewModel(
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState

    private var editingId: Long? = null
    private var selectedEpochDay: Long = LocalDate.now().toEpochDay()

    init {
        viewModelScope.launch {
            categoryDao.getAll() // Flow<List<CategoryEntity>>
                .catch {
                    _uiState.value = _uiState.value.copy(categories = emptyList())
                }
                .collect { cats: List<CategoryEntity> ->
                    val mapped = cats.map { c ->
                        val emoji = c.emoji.takeIf { it.isNotBlank() }
                        val name = c.name.ifBlank { "Category" }
                        val label = if (emoji != null) "$emoji $name" else name
                        CategoryOptionUi(id = c.id, label = label)
                    }

                    val current = _uiState.value
                    val keepSelected = current.categoryId != null && mapped.any { it.id == current.categoryId }

                    _uiState.value = current.copy(
                        categories = mapped,
                        categoryId = if (keepSelected) current.categoryId else mapped.firstOrNull()?.id
                    )
                }
        }
    }

    fun loadForEdit(id: Long) {
        editingId = id
        _uiState.value = _uiState.value.copy(isEdit = true, error = null)

        viewModelScope.launch {
            val tx = transactionDao.getById(id)
            if (tx == null) {
                _uiState.value = _uiState.value.copy(error = "Transaction not found")
                return@launch
            }

            selectedEpochDay = tx.epochDay

            _uiState.value = _uiState.value.copy(
                isEdit = true,
                type = tx.type,
                amountText = centsToDisplay(tx.amountCents),
                categoryId = tx.categoryId,
                note = tx.note
            )
        }
    }

    fun setType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type, error = null)
    }

    fun setAmount(text: String) {
        _uiState.value = _uiState.value.copy(amountText = text, error = null)
    }

    fun setCategory(id: Long) {
        _uiState.value = _uiState.value.copy(categoryId = id, error = null)
    }

    fun setNote(text: String) {
        _uiState.value = _uiState.value.copy(note = text, error = null)
    }

    fun setEpochDay(epochDay: Long) {
        selectedEpochDay = epochDay
    }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value

        val cents = dollarsTextToCents(state.amountText)
        if (cents == null || cents <= 0L) {
            _uiState.value = _uiState.value.copy(error = "Enter a valid amount (example: 100 or 100.50).")
            return
        }

        val catId = state.categoryId
        if (catId == null) {
            _uiState.value = _uiState.value.copy(error = "Choose a category.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                val entity = TransactionEntity(
                    id = editingId ?: 0L,
                    type = state.type,
                    amountCents = cents, // ✅ ALWAYS store cents
                    categoryId = catId,
                    note = state.note.ifBlank { "" },
                    epochDay = selectedEpochDay
                )

                transactionDao.insert(entity) // REPLACE works as update if same id

                _uiState.value = _uiState.value.copy(isSaving = false)
                onDone()
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = t.message ?: "Save failed"
                )
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId ?: return
        _uiState.value = _uiState.value.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                transactionDao.deleteById(id)
                _uiState.value = _uiState.value.copy(isSaving = false)
                onDone()
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = t.message ?: "Delete failed"
                )
            }
        }
    }

    /**
     * Same conversion logic as AddTransactionViewModel.
     */
    private fun dollarsTextToCents(input: String): Long? {
        val raw = input.trim()
        if (raw.isEmpty()) return null

        val cleaned = raw
            .replace("$", "")
            .replace(",", "")
            .trim()

        if (cleaned.startsWith("-")) return null

        return try {
            val bd = BigDecimal(cleaned)
                .setScale(2, RoundingMode.HALF_UP)

            bd.movePointRight(2).longValueExact()
        } catch (_: Throwable) {
            null
        }
    }

    private fun centsToDisplay(cents: Long): String {
        val absValue = abs(cents)
        val major = absValue / 100
        val minor = absValue % 100
        return "$major.${minor.toString().padStart(2, '0')}"
    }
}
