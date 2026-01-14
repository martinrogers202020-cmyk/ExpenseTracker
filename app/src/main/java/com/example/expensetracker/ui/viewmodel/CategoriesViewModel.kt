package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val repo: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        // 1) seed defaults
        viewModelScope.launch {
            runCatching { repo.seedDefaultCategoriesIfEmpty() }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to seed default categories") }
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
                    _uiState.update { it.copy(error = e.message ?: "Failed to add category") }
                }
        }
    }

    fun updateCategory(id: Long, name: String, emoji: String) {
        viewModelScope.launch {
            val old = _uiState.value.categories.firstOrNull { it.id == id }
            val keepDefault = old?.isDefault ?: false

            runCatching { repo.updateCategory(id = id, name = name, emoji = emoji, isDefault = keepDefault) }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to update category") }
                }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            runCatching { repo.deleteCategory(id) }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to delete category") }
                }
        }
    }
}
