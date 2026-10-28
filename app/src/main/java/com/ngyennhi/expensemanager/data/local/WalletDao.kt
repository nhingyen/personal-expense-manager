package com.ngyennhi.expensemanager.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY id")
    fun observeAll(): Flow<List<WalletEntity>>

    @Insert
    suspend fun insert(wallet: WalletEntity): Long

    @Update
    suspend fun update(wallet: WalletEntity): Int

    @Query("DELETE FROM wallets WHERE id = :id AND id != 1 AND NOT EXISTS (SELECT 1 FROM transactions WHERE wallet_id = :id)")
    suspend fun deleteEmpty(id: Long): Int
}
