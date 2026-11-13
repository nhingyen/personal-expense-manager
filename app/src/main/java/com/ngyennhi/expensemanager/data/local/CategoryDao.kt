package com.ngyennhi.expensemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Locale

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY type, normalized_name, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND normalized_name = :normalizedName LIMIT 1")
    suspend fun find(type: String, normalizedName: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name, normalized_name = :normalizedName WHERE id = :id")
    suspend fun updateName(id: Long, name: String, normalizedName: String): Int

    @Query("UPDATE transactions SET category = :newName WHERE type = :type AND category = :oldName")
    suspend fun updateTransactionName(type: String, oldName: String, newName: String): Int

    @Query("SELECT DISTINCT category FROM transactions WHERE type = :type")
    suspend fun transactionCategoryNames(type: String): List<String>

    @Query("UPDATE categories SET is_archived = :archived WHERE id = :id")
    suspend fun updateArchived(id: Long, archived: Boolean): Int

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Transaction
    suspend fun createIfAbsent(entity: CategoryEntity): Long {
        if (find(entity.type, entity.normalizedName) != null) return CREATE_DUPLICATE
        return insert(entity)
    }

    /** Returns the ID, or a negative result constant. Room rolls back both updates on failure. */
    @Transaction
    suspend fun renameAndUpdateTransactions(id: Long, name: String, normalizedName: String): Long {
        val existing = findById(id) ?: return NOT_FOUND
        if (find(existing.type, normalizedName)?.id?.let { it != id } == true) return CREATE_DUPLICATE
        updateName(id, name, normalizedName)
        transactionCategoryNames(existing.type)
            .filter { it.trim().lowercase(Locale.ROOT) == existing.normalizedName }
            .forEach { updateTransactionName(existing.type, it, name) }
        return id
    }

    @Transaction
    suspend fun deleteIfUnused(id: Long): Int {
        val existing = findById(id) ?: return DELETE_NOT_FOUND
        val used = transactionCategoryNames(existing.type)
            .any { it.trim().lowercase(Locale.ROOT) == existing.normalizedName }
        if (used) return DELETE_IN_USE
        return if (deleteById(id) > 0) DELETE_DELETED else DELETE_NOT_FOUND
    }

    companion object {
        const val CREATE_DUPLICATE = -1L
        const val NOT_FOUND = -2L
        const val DELETE_NOT_FOUND = 0
        const val DELETE_DELETED = 1
        const val DELETE_IN_USE = 2
    }
}
