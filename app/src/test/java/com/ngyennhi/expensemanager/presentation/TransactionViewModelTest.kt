package com.ngyennhi.expensemanager.presentation

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.domain.usecase.AddTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.DeleteTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.UpdateTransactionUseCase
import com.ngyennhi.expensemanager.presentation.transaction.ActionError
import com.ngyennhi.expensemanager.presentation.transaction.FieldError
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.transaction.TransactionFilter
import com.ngyennhi.expensemanager.presentation.transaction.TransactionNavigationEvent
import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelTest {
    private val repository = FakeRepository()
    private lateinit var viewModel: TransactionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = newViewModel()
    }

    private fun newViewModel() = TransactionViewModel(
            ObserveTransactionsUseCase(repository),
            AddTransactionUseCase(repository),
            UpdateTransactionUseCase(repository),
            DeleteTransactionUseCase(repository),
        )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun firstObservationSeparatesLoadingFromEmptyAndRetryRecovers() = runTest {
        repository.observationGate = CompletableDeferred()
        viewModel = newViewModel()
        assertEquals(LoadStatus.LOADING, viewModel.uiState.value.loadStatus)
        runCurrent()
        assertEquals(LoadStatus.LOADING, viewModel.uiState.value.loadStatus)
        repository.observationGate?.complete(Unit)
        runCurrent()
        assertEquals(LoadStatus.READY, viewModel.uiState.value.loadStatus)
        assertTrue(viewModel.uiState.value.transactions.isEmpty())

        repository.failObservation = true
        viewModel.retryObservation()
        runCurrent()
        assertEquals(LoadStatus.ERROR, viewModel.uiState.value.loadStatus)

        repository.failObservation = false
        viewModel.retryObservation()
        runCurrent()
        assertEquals(LoadStatus.READY, viewModel.uiState.value.loadStatus)
    }

    @Test
    fun searchTypeAndMonthFiltersKeepStoredRecordsUntouched() = runTest {
        val october = LocalDate.of(2026, 10, 19)
        repository.records.value = listOf(
            transaction(1, TransactionType.EXPENSE, "Ăn uống", october, "Cà phê sáng"),
            transaction(2, TransactionType.INCOME, "Lương", october, "Tháng 10"),
            transaction(3, TransactionType.EXPENSE, "Food", LocalDate.of(2026, 9, 30), null),
        )
        runCurrent()
        viewModel.selectMonth(YearMonth.of(2026, 10))
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.visibleTransactions.map { it.id })
        viewModel.setFilter(TransactionFilter.EXPENSE)
        viewModel.setQuery("CÀ PHÊ")
        assertEquals(listOf(1L), viewModel.uiState.value.visibleTransactions.map { it.id })
        viewModel.setQuery("missing")
        assertTrue(viewModel.uiState.value.visibleTransactions.isEmpty())
        assertTrue(viewModel.uiState.value.hasRecordsInMonth)
        viewModel.changeMonth(-1)
        viewModel.setQuery("")
        assertEquals(listOf(3L), viewModel.uiState.value.visibleTransactions.map { it.id })
        assertEquals(3, repository.records.value.size)
    }

    @Test
    fun invalidAmountsAndCategoryNeverWrite() = runTest {
        val invalidAmounts = listOf("", "0", "-1", "1.5", "9223372036854775808")
        for (input in invalidAmounts) {
            viewModel.startAdd()
            viewModel.setAmount(input)
            viewModel.setCategory("Food")
            viewModel.save()
            runCurrent()
            assertTrue(viewModel.uiState.value.form.amountError != null)
        }
        viewModel.startAdd()
        viewModel.setAmount("1")
        viewModel.setCategory("  ")
        viewModel.save()
        runCurrent()
        assertEquals(FieldError.REQUIRED, viewModel.uiState.value.form.categoryError)
        assertEquals(0, repository.addCalls)
    }

    @Test
    fun validSaveOccursOnceAndAppearsInObservedList() = runTest {
        runCurrent()
        repository.addGate = CompletableDeferred()
        viewModel.startAdd()
        viewModel.setType(TransactionType.INCOME)
        viewModel.setAmount(Long.MAX_VALUE.toString())
        viewModel.setCategory("Salary")
        viewModel.setDate(LocalDate.of(2026, 9, 2))
        viewModel.save()
        viewModel.save()
        runCurrent()
        assertTrue(viewModel.uiState.value.form.isSaving)
        assertEquals(1, repository.addCalls)

        repository.addGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.form.isSaving)
        assertEquals(1, repository.records.value.size)
        assertEquals(Long.MAX_VALUE, repository.records.value.single().amountVnd)
        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.selectedMonth)
        assertEquals(TransactionNavigationEvent.SAVED, viewModel.navigationEvents.first())
    }

    @Test
    fun editKeepsIdAndFailedWritesKeepInput() = runTest {
        val saved = transaction(7, TransactionType.EXPENSE, "Food", LocalDate.of(2026, 10, 19), null)
        repository.records.value = listOf(saved)
        runCurrent()
        viewModel.startEdit(7)
        viewModel.setAmount("250")
        viewModel.setNote("Lunch")
        viewModel.save()
        advanceUntilIdle()
        assertEquals(1, repository.records.value.size)
        assertEquals(7L, repository.records.value.single().id)
        assertEquals(250L, repository.records.value.single().amountVnd)
        assertEquals("Lunch", repository.records.value.single().note)

        repository.failWrite = true
        viewModel.setAmount("300")
        viewModel.save()
        advanceUntilIdle()
        assertEquals(ActionError.SAVE_FAILED, viewModel.uiState.value.actionError)
        assertEquals("300", viewModel.uiState.value.form.amountInput)
        assertEquals(250L, repository.records.value.single().amountVnd)
    }

    @Test
    fun unknownUpdateAndDeleteAreNotReportedAsSuccess() = runTest {
        val saved = transaction(7, TransactionType.EXPENSE, "Food", LocalDate.of(2026, 10, 19), null)
        repository.records.value = listOf(saved)
        runCurrent()
        viewModel.startEdit(7)
        repository.updateResult = false
        viewModel.save()
        advanceUntilIdle()
        assertEquals(ActionError.RECORD_UNAVAILABLE, viewModel.uiState.value.actionError)

        viewModel.selectRecord(7)
        repository.deleteResult = false
        viewModel.deleteSelected()
        advanceUntilIdle()
        assertEquals(ActionError.RECORD_UNAVAILABLE, viewModel.uiState.value.actionError)
        assertEquals(listOf(saved), repository.records.value)
    }

    private fun transaction(id: Long, type: TransactionType, category: String, date: LocalDate, note: String?) =
        Transaction(id, type, 100, category, date, note)

    private class FakeRepository : TransactionRepository {
        val records = MutableStateFlow<List<Transaction>>(emptyList())
        var addCalls = 0
        var addGate: CompletableDeferred<Unit>? = null
        var observationGate: CompletableDeferred<Unit>? = null
        var failObservation = false
        var failWrite = false
        var updateResult = true
        var deleteResult = true

        override fun observeTransactions(): Flow<List<Transaction>> = flow {
            observationGate?.await()
            if (failObservation) throw IllegalStateException("Read failed")
            emitAll(records)
        }

        override suspend fun add(transaction: TransactionDraft): Long {
            addCalls++
            addGate?.await()
            if (failWrite) throw IllegalStateException("Write failed")
            val id = (records.value.maxOfOrNull { it.id } ?: 0) + 1
            records.value = listOf(Transaction(id, transaction.type, transaction.amountVnd,
                transaction.category, transaction.date, transaction.note)) + records.value
            return id
        }

        override suspend fun update(transaction: Transaction): Boolean {
            if (failWrite) throw IllegalStateException("Write failed")
            if (!updateResult || records.value.none { it.id == transaction.id }) return false
            records.value = records.value.map { if (it.id == transaction.id) transaction else it }
            return true
        }

        override suspend fun delete(id: Long): Boolean {
            if (failWrite) throw IllegalStateException("Write failed")
            if (!deleteResult || records.value.none { it.id == id }) return false
            records.value = records.value.filterNot { it.id == id }
            return true
        }
    }
}
