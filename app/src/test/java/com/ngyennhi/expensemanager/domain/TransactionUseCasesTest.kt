package com.ngyennhi.expensemanager.domain

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.domain.usecase.AddTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.DeleteTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.UpdateTransactionUseCase
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TransactionUseCasesTest {
    private val date = LocalDate.of(2026, 10, 19)
    private val repository = FakeTransactionRepository()
    private val add = AddTransactionUseCase(repository)
    private val update = UpdateTransactionUseCase(repository)
    private val delete = DeleteTransactionUseCase(repository)
    private val observe = ObserveTransactionsUseCase(repository)

    @Test
    fun addAcceptsPositiveWholeVndAndReturnsCreatedId() = runBlocking {
        val smallest = draft(amountVnd = 1)
        val largest = draft(amountVnd = Long.MAX_VALUE, type = TransactionType.INCOME)

        assertEquals(37L, add(smallest))
        assertEquals(37L, add(largest))
        assertEquals(listOf(smallest, largest), repository.added)
    }

    @Test
    fun addAndUpdateRejectInvalidAmountsAndCategoriesBeforeWriting() = runBlocking {
        val invalid = listOf(
            draft(amountVnd = 0),
            draft(amountVnd = -1),
            draft(category = ""),
            draft(category = " \t "),
        )

        for (candidate in invalid) {
            expectInvalid { add(candidate) }
            expectInvalid { update(candidate.saved()) }
        }

        assertTrue(repository.added.isEmpty())
        assertTrue(repository.updated.isEmpty())
    }

    @Test
    fun updateAndDeleteForwardResultsIncludingUnknownIds() = runBlocking {
        val saved = draft().saved()
        assertTrue(update(saved))
        assertTrue(delete(saved.id))
        repository.writeResult = false
        assertFalse(update(saved))
        assertFalse(delete(999L))
        assertEquals(listOf(saved, saved), repository.updated)
        assertEquals(listOf(saved.id, 999L), repository.deleted)
    }

    @Test
    fun observePassesThroughEmptyAndChangedLists() = runBlocking {
        assertEquals(emptyList<Transaction>(), observe().first())
        val saved = draft().saved()
        repository.records.value = listOf(saved)
        assertEquals(listOf(saved), observe().first())
    }

    private fun draft(
        amountVnd: Long = 500,
        category: String = "Food",
        type: TransactionType = TransactionType.EXPENSE,
    ) = TransactionDraft(type, amountVnd, category, date, null)

    private fun TransactionDraft.saved() = Transaction(12L, type, amountVnd, category, date, note)

    private suspend fun expectInvalid(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected invalid transaction")
        } catch (_: IllegalArgumentException) {
            // The repository must not be called for invalid input.
        }
    }

    private class FakeTransactionRepository : TransactionRepository {
        val records = MutableStateFlow<List<Transaction>>(emptyList())
        val added = mutableListOf<TransactionDraft>()
        val updated = mutableListOf<Transaction>()
        val deleted = mutableListOf<Long>()
        var writeResult = true

        override fun observeTransactions(): Flow<List<Transaction>> = records

        override suspend fun add(transaction: TransactionDraft): Long {
            added += transaction
            return 37L
        }

        override suspend fun update(transaction: Transaction): Boolean {
            updated += transaction
            return writeResult
        }

        override suspend fun delete(id: Long): Boolean {
            deleted += id
            return writeResult
        }
    }
}
