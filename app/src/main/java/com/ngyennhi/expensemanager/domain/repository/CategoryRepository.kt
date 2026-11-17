package com.ngyennhi.expensemanager.domain.repository

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

sealed interface CategorySaveResult {
    data class Saved(val id: Long) : CategorySaveResult
    data object Duplicate : CategorySaveResult
    data object NotFound : CategorySaveResult
}
enum class CategoryDeleteResult { DELETED, NOT_FOUND, IN_USE }

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun findByName(type: TransactionType, name: String): Category?
    suspend fun create(type: TransactionType, name: String): CategorySaveResult
    suspend fun rename(id: Long, name: String): CategorySaveResult
    suspend fun setArchived(id: Long, archived: Boolean): Boolean
    suspend fun delete(id: Long): CategoryDeleteResult
}
