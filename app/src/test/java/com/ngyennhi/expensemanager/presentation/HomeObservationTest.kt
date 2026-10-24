package com.ngyennhi.expensemanager.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.domain.usecase.*
import com.ngyennhi.expensemanager.presentation.home.*
import com.ngyennhi.expensemanager.presentation.transaction.TransactionFilter
import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
import java.time.YearMonth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeObservationTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repository = Repository()
    private val budgets = FakeMonthlyBudgetRepository()
    private val month = YearMonth.of(2026, 10)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(handle: SavedStateHandle = SavedStateHandle(mapOf("dashboard_selected_month" to month.toString()))) =
        HomeViewModel(ObserveTransactionsUseCase(repository), CalculateMonthlySummaryUseCase(), handle,
            ObserveMonthlyBudgetsUseCase(budgets), SaveMonthlyBudgetUseCase(budgets), CalculateMonthlyBudgetUseCase())
            .also { store.put("home", it) }

    @Test
    fun delayedFirstSnapshotNeverFabricatesZeroAndUsesLatestSelectedMonth() = runTest(dispatcher) {
        repository.gate = CompletableDeferred()
        val model = model()
        runCurrent()
        model.changeMonth(-1)
        assertEquals(HomeLoadStatus.LOADING, model.uiState.value.loadStatus)
        assertNull(model.uiState.value.summary)
        assertFalse(model.uiState.value.isEmpty)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertTrue(model.uiState.value.isEmpty)
        assertEquals(month.minusMonths(1), model.uiState.value.summary?.month)
    }

    @Test
    fun recentOrderingLimitAndTotalsUseOneSnapshotIncludingFutureDates() = runTest(dispatcher) {
        repository.records.value = (1L..7L).map { row(it, day = if (it < 4) 1 else 31) }.reversed() +
            row(99, day = 1).copy(date = month.minusMonths(1).atDay(1))
        val model = model()
        runCurrent()
        assertEquals(listOf(7L, 6L, 5L, 4L, 3L), model.uiState.value.recentTransactions.map { it.id })
        assertEquals(700L, model.uiState.value.summary?.incomeVnd)
        repeat(24) { model.changeMonth(1) }
        assertTrue(model.uiState.value.isEmpty)
        repeat(24) { model.changeMonth(-1) }
        assertEquals(700L, model.uiState.value.summary?.incomeVnd)
        assertEquals(1, repository.starts)
    }

    @Test
    fun addUpdateTypeAmountDateAndDeleteEmissionsUpdateSummaryAndRecentTogether() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        repository.records.value = listOf(row(1))
        runCurrent()
        assertEquals(100L, model.uiState.value.summary?.netVnd)
        repository.records.value = listOf(row(1).copy(type = TransactionType.EXPENSE, amountVnd = 150))
        runCurrent()
        assertEquals(-150L, model.uiState.value.summary?.netVnd)
        assertEquals(TransactionType.EXPENSE, model.uiState.value.recentTransactions.single().type)
        repository.records.value = listOf(row(1).copy(date = month.plusMonths(1).atDay(1)))
        runCurrent()
        assertTrue(model.uiState.value.isEmpty)
        model.changeMonth(1)
        assertEquals(100L, model.uiState.value.summary?.netVnd)
        repository.records.value = emptyList()
        runCurrent()
        assertTrue(model.uiState.value.isEmpty)
        assertEquals(0L, model.uiState.value.summary?.netVnd)
    }

    @Test
    fun initialFailureAndFailedRetryRemainErrorsAfterMonthChangeThenRecover() = runTest(dispatcher) {
        repository.fail = true
        val model = model()
        runCurrent()
        model.changeMonth(-1)
        assertEquals(HomeLoadStatus.ERROR, model.uiState.value.loadStatus)
        assertNull(model.uiState.value.summary)
        assertFalse(model.uiState.value.isEmpty)
        model.retryObservation()
        runCurrent()
        assertEquals(HomeError.DATA_LOAD_FAILED, model.uiState.value.error)
        repository.fail = false
        model.retryObservation()
        runCurrent()
        assertTrue(model.uiState.value.isEmpty)
        assertEquals(month.minusMonths(1), model.uiState.value.selectedMonth)
        assertNull(model.uiState.value.error)
    }

    @Test
    fun failureAfterSnapshotKeepsStaleDataAndMonthChangesNeverRelabelOldTotals() = runTest(dispatcher) {
        repository.records.value = listOf(row(1), row(2).copy(date = month.minusMonths(1).atDay(2), amountVnd = 200))
        val model = model()
        runCurrent()
        repository.fail = true
        repository.records.value = repository.records.value + row(3)
        runCurrent()
        assertTrue(model.uiState.value.isStale)
        assertEquals(100L, model.uiState.value.summary?.netVnd)
        model.changeMonth(-1)
        assertTrue(model.uiState.value.isStale)
        assertEquals(200L, model.uiState.value.summary?.netVnd)
        assertEquals(month.minusMonths(1), model.uiState.value.summary?.month)
        assertEquals(listOf(2L), model.uiState.value.recentTransactions.map { it.id })
        repository.gate = CompletableDeferred()
        model.retryObservation()
        runCurrent()
        model.changeMonth(1)
        assertTrue(model.uiState.value.isRetrying)
        assertTrue(model.uiState.value.isStale)
        repository.fail = false
        repository.gate!!.complete(Unit)
        runCurrent()
        assertFalse(model.uiState.value.isStale)
        assertFalse(model.uiState.value.isRetrying)
        assertEquals(200L, model.uiState.value.summary?.netVnd)
    }

    @Test
    fun overflowHidesValuesAndRecoversByMonthSelectionOrCorrectedRecords() = runTest(dispatcher) {
        repository.records.value = listOf(row(1).copy(amountVnd = Long.MAX_VALUE), row(2))
        val model = model()
        runCurrent()
        assertEquals(HomeError.CALCULATION_OVERFLOW, model.uiState.value.error)
        assertNull(model.uiState.value.summary)
        assertFalse(model.uiState.value.canShowData)
        model.changeMonth(1)
        assertTrue(model.uiState.value.isEmpty)
        model.changeMonth(-1)
        assertEquals(HomeError.CALCULATION_OVERFLOW, model.uiState.value.error)
        repository.records.value = listOf(row(2))
        runCurrent()
        assertNull(model.uiState.value.error)
        assertEquals(100L, model.uiState.value.summary?.netVnd)
    }

    @Test
    fun retriesReplaceCollectorAndClearingViewModelCancelsObservation() = runTest(dispatcher) {
        val model = model()
        runCurrent()
        repeat(10) { model.retryObservation(); runCurrent() }
        assertEquals(1, repository.active)
        assertEquals(1, repository.maxActive)
        store.clear()
        runCurrent()
        assertEquals(0, repository.active)
    }

    @Test
    fun monthSurvivesSavedStateRecreationAndCrossesDecemberJanuaryAndLeapFebruary() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("dashboard_selected_month" to "2023-12"))
        var model = model(handle)
        model.changeMonth(1)
        assertEquals(YearMonth.of(2024, 1), model.uiState.value.selectedMonth)
        model.changeMonth(1)
        repository.records.value = listOf(row(1).copy(date = YearMonth.of(2024, 2).atEndOfMonth()))
        runCurrent()
        model = model(SavedStateHandle(mapOf("dashboard_selected_month" to handle.get<String>("dashboard_selected_month"))))
        runCurrent()
        assertEquals(YearMonth.of(2024, 2), model.uiState.value.selectedMonth)
        assertEquals(100L, model.uiState.value.summary?.netVnd)
    }

    @Test
    fun transactionFiltersAreIndependentAndViewAllExplicitlyResetsThem() = runTest(dispatcher) {
        repository.records.value = listOf(row(1))
        val model = model()
        val transactions = TransactionViewModel(ObserveTransactionsUseCase(repository), AddTransactionUseCase(repository),
            UpdateTransactionUseCase(repository), DeleteTransactionUseCase(repository))
        store.put("transactions", transactions)
        runCurrent()
        transactions.setFilter(TransactionFilter.EXPENSE)
        transactions.setQuery("not found")
        transactions.selectMonth(month.minusMonths(1))
        assertEquals(100L, model.uiState.value.summary?.netVnd)
        assertEquals(listOf(1L), model.uiState.value.recentTransactions.map { it.id })
        transactions.showAllForMonth(month)
        assertEquals(month, transactions.uiState.value.selectedMonth)
        assertEquals("", transactions.uiState.value.query)
        assertEquals(TransactionFilter.ALL, transactions.uiState.value.filter)
        assertEquals(listOf(1L), transactions.uiState.value.visibleTransactions.map { it.id })
    }

    private fun row(id: Long, day: Int = 1) = Transaction(id, TransactionType.INCOME, 100, "User category", month.atDay(day), null)

    private class Repository : TransactionRepository {
        val records = MutableStateFlow<List<Transaction>>(emptyList())
        var gate: CompletableDeferred<Unit>? = null
        var fail = false
        var starts = 0
        var active = 0
        var maxActive = 0
        override fun observeTransactions() = flow {
            starts++
            active++
            maxActive = maxOf(maxActive, active)
            try {
                gate?.await()
                records.collect {
                    if (fail) error("Read failed")
                    emit(it)
                }
            } finally { active-- }
        }
        override suspend fun add(transaction: TransactionDraft): Long = error("Not used")
        override suspend fun update(transaction: Transaction): Boolean = error("Not used")
        override suspend fun delete(id: Long): Boolean = error("Not used")
    }
}
