package com.example.expensetracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.R
import com.example.expensetracker.data.repo.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryRowUi(
    val id: Long,
    val name: String,
    val emoji: String,
    val isDefault: Boolean
)

data class CategoriesUiState(
    val categories: List<CategoryRowUi> = emptyList(),
    val error: String? = null
)

class CategoriesViewModel(
    private val context: Context,
    private val repo: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        // 1) seed defaults
        viewModelScope.launch {
            runCatching { repo.seedDefaultCategoriesIfEmpty(defaultCategories()) }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.message ?: context.getString(R.string.categories_error_seed_defaults))
                    }
                }
        }

        // 2) observe list
        viewModelScope.launch {
            repo.observeCategories().collect { list ->
                _uiState.update { state ->
                    state.copy(
                        categories = list.map { c ->
                            CategoryRowUi(
                                id = c.id,
                                name = c.name,
                                emoji = c.emoji,
                                isDefault = c.isDefault
                            )
                        }
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            runCatching { repo.addCategory(name = name, emoji = emoji, isDefault = false) }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: context.getString(R.string.categories_error_add)) }
                }
        }
    }

    fun updateCategory(id: Long, name: String, emoji: String) {
        viewModelScope.launch {
            val old = _uiState.value.categories.firstOrNull { it.id == id }
            val keepDefault = old?.isDefault ?: false

            runCatching { repo.updateCategory(id = id, name = name, emoji = emoji, isDefault = keepDefault) }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: context.getString(R.string.categories_error_update)) }
                }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            runCatching { repo.deleteCategory(id) }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: context.getString(R.string.categories_error_delete)) }
                }
        }
    }

    private fun defaultCategories(): List<Pair<String, String>> = listOf(
        context.getString(R.string.category_default_bills) to "🧾",
        context.getString(R.string.category_default_coffee) to "☕",
        context.getString(R.string.category_default_eating_out) to "🍔",
        context.getString(R.string.category_default_groceries) to "🛒",
        context.getString(R.string.category_default_health) to "💙",
        context.getString(R.string.category_default_rent) to "🏠"
    )
}
