// FILE: app/src/main/java/com/example/expensetracker/data/repo/RecurringTransactionRepository.kt
package com.example.expensetracker.data.repo

import com.example.expensetracker.data.db.RecurringTransactionDao
import com.example.expensetracker.data.model.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepository(
    private val dao: RecurringTransactionDao
) {
    fun observeAll(): Flow<List<RecurringTransactionEntity>> = dao.observeAll()

    suspend fun getAllOnce(): List<RecurringTransactionEntity> = dao.getAllOnce()

    suspend fun getDue(todayEpoch: Long): List<RecurringTransactionEntity> =
        dao.getDue(todayEpochDay = todayEpoch)

    suspend fun countDue(todayEpoch: Long): Int =
        dao.countDue(todayEpochDay = todayEpoch)

    suspend fun upsert(entity: RecurringTransactionEntity): Long =
        dao.upsert(entity)

    suspend fun upsertAll(items: List<RecurringTransactionEntity>) =
        dao.upsertAll(items)

    suspend fun delete(entity: RecurringTransactionEntity) =
        dao.delete(entity)

    suspend fun clearAll() =
        dao.clearAll()
}
