package com.example.expensetracker.data.repo

import com.example.expensetracker.data.db.BudgetDao
import com.example.expensetracker.data.model.BudgetEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(
    private val dao: BudgetDao
) {
    fun observeForMonth(year: Int, month: Int): Flow<List<BudgetEntity>> =
        dao.observeForMonth(year, month)

    suspend fun upsert(budget: BudgetEntity): Long = dao.upsert(budget)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
