package com.ngyennhi.expensemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Locale

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date_epoch_day DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun updateRaw(transaction: TransactionEntity): Int

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND normalized_name = :normalizedName LIMIT 1")
    suspend fun findCategory(type: String, normalizedName: String): CategoryEntity?

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Transaction
    suspend fun insertWithCategory(transaction: TransactionEntity, normalizedName: String): Long {
        val existing = findCategory(transaction.type, normalizedName)
        if (existing?.isArchived == true) return CATEGORY_ARCHIVED
        val categoryName = existing?.name ?: transaction.category.trim().also { name ->
            insertCategory(CategoryEntity(type = transaction.type, name = name,
                normalizedName = normalizedName))
        }
        return insert(transaction.copy(category = categoryName))
    }

    @Transaction
    suspend fun updateWithCategory(transaction: TransactionEntity, normalizedName: String): Int {
        val current = findById(transaction.id) ?: return RECORD_NOT_FOUND
        val existing = findCategory(transaction.type, normalizedName)
        val unchangedHistoricalCategory = current.type == transaction.type &&
            current.category.trim().lowercase(Locale.ROOT) == normalizedName
        if (existing?.isArchived == true && !unchangedHistoricalCategory) return CATEGORY_ARCHIVED.toInt()
        val categoryName = if (unchangedHistoricalCategory) current.category else {
            existing?.name ?: transaction.category.trim().also { name ->
                insertCategory(CategoryEntity(type = transaction.type, name = name,
                    normalizedName = normalizedName))
            }
        }
        val updated = updateRaw(transaction.copy(category = categoryName))
        check(updated > 0) { "Transaction disappeared during an atomic update" }
        return updated
    }

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    companion object {
        const val CATEGORY_ARCHIVED = -1L
        const val RECORD_NOT_FOUND = 0
    }
}
