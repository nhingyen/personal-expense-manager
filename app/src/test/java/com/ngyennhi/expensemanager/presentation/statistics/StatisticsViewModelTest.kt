package com.ngyennhi.expensemanager.presentation.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlyStatisticsUseCase
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlySummaryUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val month = YearMonth.of(2026, 1)
    private val repository = StatisticsRepository()
    private val store = ViewModelStore()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun create(handle: SavedStateHandle = SavedStateHandle(mapOf("statistics_selected_month" to month.toString()))): StatisticsViewModel =
        StatisticsViewModel(ObserveTransactionsUseCase(repository),
            CalculateMonthlyStatisticsUseCase(CalculateMonthlySummaryUseCase()), handle).also {
                store.put("statistics-${System.identityHashCode(it)}", it)
            }

    @Test
    fun loadingIsDistinctFromObservedEmptyAndIncomeOnly() = runTest(dispatcher) {
        val vm = create()
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.statistics)
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(0L, vm.uiState.value.statistics?.summary?.expenseVnd)
        assertTrue(vm.uiState.value.statistics!!.categories.isEmpty())
        repository.values.value = listOf(record(25, TransactionType.INCOME))
        runCurrent()
        assertEquals(25L, vm.uiState.value.statistics?.summary?.incomeVnd)
        assertTrue(vm.uiState.value.statistics!!.categories.isEmpty())
    }

    @Test
    fun monthChangesUseOneObserverAndRestoreMonthAcrossYearBoundary() = runTest(dispatcher) {
        repository.values.value = listOf(record(10), record(20, dateMonth = month.minusMonths(1)))
        val handle = SavedStateHandle(mapOf("statistics_selected_month" to month.toString()))
        val vm = create(handle)
        runCurrent()
        vm.changeMonth(-1)
        assertEquals(YearMonth.of(2025, 12), vm.uiState.value.selectedMonth)
        assertEquals(20L, vm.uiState.value.statistics?.summary?.expenseVnd)
        assertEquals("2025-12", handle.get<String>("statistics_selected_month"))
        vm.changeMonth(1)
        assertEquals(10L, vm.uiState.value.statistics?.summary?.expenseVnd)
        assertEquals(1, repository.starts)
        val restored = create(SavedStateHandle(mapOf("statistics_selected_month" to handle.get<String>("statistics_selected_month"))))
        runCurrent()
        assertEquals(vm.uiState.value.selectedMonth, restored.uiState.value.selectedMonth)
    }

    @Test
    fun snapshotsReflectCreateEditAndDeleteInTotalsSlicesAndComparison() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        repository.values.value = listOf(record(10))
        runCurrent()
        assertEquals(10L, vm.uiState.value.statistics?.categories?.single()?.amountVnd)
        repository.values.value = listOf(record(40, TransactionType.INCOME))
        runCurrent()
        assertEquals(40L, vm.uiState.value.statistics?.comparison?.last()?.incomeVnd)
        assertEquals(0L, vm.uiState.value.statistics?.summary?.expenseVnd)
        assertTrue(vm.uiState.value.statistics!!.categories.isEmpty())
        repository.values.value = emptyList()
        runCurrent()
        assertEquals(0L, vm.uiState.value.statistics?.summary?.incomeVnd)
    }

    @Test
    fun readFailureClearsDataAndRetryReplacesObserverWithoutStaleMonthValues() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        repository.values.value = listOf(record(10))
        runCurrent()
        repository.fail = true
        repository.values.value = listOf(record(20))
        runCurrent()
        assertEquals(StatisticsError.LOAD_FAILED, vm.uiState.value.error)
        assertNull(vm.uiState.value.statistics)
        vm.changeMonth(-1)
        assertEquals(StatisticsError.LOAD_FAILED, vm.uiState.value.error)
        assertNull(vm.uiState.value.statistics)
        repository.fail = false
        vm.retryObservation()
        assertTrue(vm.uiState.value.isLoading)
        vm.retryObservation()
        runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(0L, vm.uiState.value.statistics?.summary?.expenseVnd)
        assertEquals(1, repository.active)
        assertEquals(1, repository.maxActive)
    }

    @Test
    fun firstReadFailureCanRetryAndChangeMonthWhileLoading() = runTest(dispatcher) {
        repository.fail = true
        val vm = create()
        vm.changeMonth(-1)
        assertTrue(vm.uiState.value.isLoading)
        runCurrent()
        assertEquals(StatisticsError.LOAD_FAILED, vm.uiState.value.error)
        repository.fail = false
        vm.retryObservation()
        runCurrent()
        assertEquals(YearMonth.of(2025, 12), vm.uiState.value.statistics?.summary?.month)
    }

    @Test
    fun overflowClearsChartsAndValidSnapshotOrMonthRecovers() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        repository.values.value = listOf(record(Long.MAX_VALUE), record(1))
        runCurrent()
        assertEquals(StatisticsError.CALCULATION_OVERFLOW, vm.uiState.value.error)
        assertNull(vm.uiState.value.statistics)
        vm.changeMonth(-1)
        assertNull(vm.uiState.value.error)
        vm.changeMonth(1)
        assertEquals(StatisticsError.CALCULATION_OVERFLOW, vm.uiState.value.error)
        repository.values.value = listOf(record(5))
        runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(5L, vm.uiState.value.statistics?.summary?.expenseVnd)
    }

    @Test
    fun cancellationIsNotReportedAsLoadFailureAndClearingStopsCollector() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        vm.retryObservation()
        runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(1, repository.active)
        repository.cancel = true
        repository.values.value = listOf(record(1))
        runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(0, repository.active)
        repository.cancel = false
        vm.retryObservation()
        runCurrent()
        store.clear()
        runCurrent()
        assertEquals(0, repository.active)
    }

    @Test
    fun invalidSavedMonthFallsBackToCurrentMonth() = runTest(dispatcher) {
        val vm = create(SavedStateHandle(mapOf("statistics_selected_month" to "invalid")))
        assertEquals(YearMonth.now(), vm.uiState.value.selectedMonth)
        runCurrent()
    }

    private fun record(amount: Long, type: TransactionType = TransactionType.EXPENSE,
        dateMonth: YearMonth = month) = Transaction(1, type, amount, "Food", dateMonth.atDay(1), null)
}

private class StatisticsRepository : TransactionRepository {
    val values = MutableStateFlow<List<Transaction>>(emptyList())
    var fail = false
    var cancel = false
    var starts = 0
    var active = 0
    var maxActive = 0
    override fun observeTransactions(): Flow<List<Transaction>> = flow {
        starts++
        active++
        maxActive = maxOf(maxActive, active)
        try {
            values.collect {
                if (fail) error("Read failed")
                if (cancel) throw CancellationException("Cancelled")
                emit(it)
            }
        } finally { active-- }
    }
    override suspend fun add(transaction: TransactionDraft): Long = error("Not used")
    override suspend fun update(transaction: Transaction): Boolean = error("Not used")
    override suspend fun delete(id: Long): Boolean = error("Not used")
}
