package com.example.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expensetracker.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY epochDay DESC, id DESC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionEntity>): List<Long>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countByCategoryId(categoryId: Long): Int

    @Query(
        """
        SELECT * FROM transactions
        WHERE epochDay BETWEEN :startDay AND :endDay
        ORDER BY epochDay ASC, id ASC
        """
    )
    fun observeBetweenDays(startDay: Long, endDay: Long): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY epochDay ASC, id ASC
        """
    )
    suspend fun getBetween(startEpochDay: Long, endEpochDay: Long): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE epochDay BETWEEN :startEpochDay AND :endEpochDay
          AND type = :type
        ORDER BY epochDay ASC, id ASC
        """
    )
    suspend fun getBetweenByType(
        startEpochDay: Long,
        endEpochDay: Long,
        type: String
    ): List<TransactionEntity>
}
