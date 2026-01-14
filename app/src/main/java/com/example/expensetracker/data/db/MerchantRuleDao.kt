package com.example.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.expensetracker.data.model.MerchantRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {

    @Query("SELECT * FROM merchant_rules ORDER BY priority DESC, createdAtEpochMs DESC, id DESC")
    fun observeAll(): Flow<List<MerchantRuleEntity>>

    @Query("SELECT * FROM merchant_rules ORDER BY priority DESC, createdAtEpochMs DESC, id DESC")
    suspend fun getAllOnce(): List<MerchantRuleEntity>

    @Query("SELECT * FROM merchant_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MerchantRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<MerchantRuleEntity>): List<Long>

    @Query("DELETE FROM merchant_rules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM merchant_rules")
    suspend fun deleteAll()
}
