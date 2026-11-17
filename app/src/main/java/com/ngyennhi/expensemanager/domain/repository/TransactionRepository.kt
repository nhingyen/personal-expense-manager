package com.ngyennhi.expensemanager.domain.repository

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>

    suspend fun add(transaction: TransactionDraft): Long

    suspend fun update(transaction: Transaction): Boolean

    suspend fun delete(id: Long): Boolean
}

/** Expected write rejection when new free text resolves to an archived catalog row. */
class ArchivedCategoryException : IllegalStateException("Category is archived")
