package com.ngyennhi.expensemanager.data.repository

import com.ngyennhi.expensemanager.data.local.TransactionDao
import com.ngyennhi.expensemanager.data.mapper.toDomain
import com.ngyennhi.expensemanager.data.mapper.toEntity
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
) : TransactionRepository {
    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun add(transaction: TransactionDraft): Long =
        dao.insert(transaction.toEntity())

    override suspend fun update(transaction: Transaction): Boolean =
        dao.update(transaction.toEntity()) > 0

    override suspend fun delete(id: Long): Boolean = dao.deleteById(id) > 0
}
