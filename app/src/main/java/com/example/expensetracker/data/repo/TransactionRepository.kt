package com.example.expensetracker.data.repo

import com.example.expensetracker.data.db.TransactionDao
import com.example.expensetracker.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val dao: TransactionDao
) {
    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll()

    fun observeTransactions(): Flow<List<TransactionEntity>> = dao.observeAll()

    suspend fun insert(tx: TransactionEntity): Long = dao.insert(tx)

    suspend fun getById(id: Long): TransactionEntity? = dao.getById(id)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun countByCategoryId(categoryId: Long): Int = dao.countByCategoryId(categoryId)

    fun observeBetweenDays(startDay: Long, endDay: Long): Flow<List<TransactionEntity>> =
        dao.observeBetweenDays(startDay, endDay)
}
