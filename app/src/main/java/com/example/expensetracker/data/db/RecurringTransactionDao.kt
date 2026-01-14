// FILE: app/src/main/java/com/example/expensetracker/data/db/RecurringTransactionDao.kt
package com.example.expensetracker.data.db

import androidx.room.*
import com.example.expensetracker.data.model.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueEpochDay ASC")
    fun observeAll(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions ORDER BY nextDueEpochDay ASC")
    suspend fun getAllOnce(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<RecurringTransactionEntity>)

    @Delete
    suspend fun delete(entity: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAll()

    // Due items (for engine/worker)
    @Query("""
        SELECT * FROM recurring_transactions 
        WHERE isActive = 1 AND nextDueEpochDay <= :todayEpochDay
        ORDER BY nextDueEpochDay ASC
    """)
    suspend fun getDue(todayEpochDay: Long): List<RecurringTransactionEntity>

    @Query("""
        SELECT COUNT(*) FROM recurring_transactions
        WHERE isActive = 1 AND nextDueEpochDay <= :todayEpochDay
    """)
    suspend fun countDue(todayEpochDay: Long): Int
}
