package com.example.expensetracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.db.AppDatabase
import com.example.expensetracker.data.model.CategoryEntity
import com.example.expensetracker.data.model.MerchantRuleEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MerchantRulesState(
    val rules: List<MerchantRuleEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)

class MerchantRulesViewModel(
    private val db: AppDatabase
) : ViewModel() {

    private val rulesFlow = db.merchantRuleDao().observeAll()
    private val catsFlow = db.categoryDao().getAll()

    val state: StateFlow<MerchantRulesState> =
        combine(rulesFlow, catsFlow) { rules, cats ->
            MerchantRulesState(rules = rules, categories = cats)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MerchantRulesState())

    fun upsert(rule: MerchantRuleEntity) {
        viewModelScope.launch { db.merchantRuleDao().upsert(rule) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { db.merchantRuleDao().deleteById(id) }
    }

    fun toggle(rule: MerchantRuleEntity, enabled: Boolean) {
        upsert(rule.copy(enabled = enabled))
    }
}
