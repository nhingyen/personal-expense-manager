package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

class AddTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: TransactionDraft): Long {
        validateTransaction(transaction.amountVnd, transaction.category)
        return repository.add(transaction)
    }
}

class UpdateTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction): Boolean {
        validateTransaction(transaction.amountVnd, transaction.category)
        return repository.update(transaction)
    }
}

class DeleteTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: Long): Boolean = repository.delete(id)
}

class ObserveTransactionsUseCase(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> = repository.observeTransactions()
}

private fun validateTransaction(amountVnd: Long, category: String) {
    require(amountVnd > 0) { "Amount must be positive VND" }
    require(category.isNotBlank()) { "Category must not be blank" }
}
