package com.ngyennhi.expensemanager.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.domain.usecase.*
import com.ngyennhi.expensemanager.presentation.dashboard.*
import java.time.YearMonth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardBudgetTest {
    private val dispatcher = StandardTestDispatcher()
    private val budgets = FakeMonthlyBudgetRepository()
    private val records = MutableStateFlow<List<Transaction>>(emptyList())
    private val month = YearMonth.of(2026, 10)
    private val store = ViewModelStore()
    private var failSource = false
    private val transactions = object : TransactionRepository {
        override fun observeTransactions() = records.map { if (failSource) error("Read failed") else it }
        override suspend fun add(transaction: TransactionDraft): Long = error("Not used")
        override suspend fun update(transaction: Transaction): Boolean = error("Not used")
        override suspend fun delete(id: Long): Boolean = error("Not used")
    }
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun model() = DashboardViewModel(ObserveTransactionsUseCase(transactions), CalculateMonthlySummaryUseCase(),
        SavedStateHandle(mapOf("dashboard_selected_month" to month.toString())), ObserveMonthlyBudgetsUseCase(budgets),
        SaveMonthlyBudgetUseCase(budgets), CalculateMonthlyBudgetUseCase()).also { store.put("dashboard", it) }

    @Test fun loadingUnsetAndConfiguredLimitsAreDistinctAndReactToExpenseUpdates() = runTest(dispatcher) {
        budgets.gate = CompletableDeferred()
        val model = model()
        runCurrent()
        assertEquals(DashboardLoadStatus.LOADING, model.uiState.value.budgetLoadStatus)
        assertNull(model.uiState.value.budget)
        budgets.gate!!.complete(Unit)
        runCurrent()
        assertEquals(DashboardLoadStatus.READY, model.uiState.value.budgetLoadStatus)
        assertNull(model.uiState.value.budgetLimitVnd)
        budgets.limits.value = mapOf(month to 100L)
        runCurrent()
        assertEquals(100L, model.uiState.value.budget?.remainingVnd)
        records.value = listOf(Transaction(1, TransactionType.EXPENSE, 150, "Food", month.atDay(1), null))
        runCurrent()
        assertEquals(-50L, model.uiState.value.budget?.remainingVnd)
        records.value = listOf(records.value.single().copy(type = TransactionType.INCOME))
        runCurrent()
        assertEquals(100L, model.uiState.value.budget?.remainingVnd)
        assertEquals(listOf(DashboardCategory("Food", TransactionType.INCOME)), model.uiState.value.categories)
        records.value = emptyList()
        runCurrent()
        assertEquals(0L, model.uiState.value.budget?.spentVnd)
    }

    @Test fun monthSelectionNeverMixesLimitsOrExpenseSnapshots() = runTest(dispatcher) {
        budgets.limits.value = mapOf(month to 100L, month.plusMonths(1) to 300L)
        records.value = listOf(Transaction(1, TransactionType.EXPENSE, 150, "Food", month.atDay(1), null))
        val model = model(); runCurrent()
        assertEquals(-50L, model.uiState.value.budget?.remainingVnd)
        model.changeMonth(1)
        assertEquals(month.plusMonths(1), model.uiState.value.budget?.month)
        assertEquals(300L, model.uiState.value.budget?.remainingVnd)
        model.changeMonth(1)
        assertNull(model.uiState.value.budget)
        assertNull(model.uiState.value.budgetLimitVnd)
        assertEquals(1, budgets.maxActive)
    }

    @Test fun validationAndFailedWritesKeepInputAndDuplicateSaveIsBlocked() = runTest(dispatcher) {
        val model = model(); runCurrent()
        for (invalid in listOf("", "0", "-1", "1.5", "9223372036854775808")) {
            model.setBudgetInput(invalid); model.saveMonthlyBudget(); runCurrent()
            assertTrue(model.uiState.value.budgetInputInvalid)
        }
        assertEquals(0, budgets.writes)
        budgets.failWrite = true
        model.setBudgetInput("12000000"); model.saveMonthlyBudget(); runCurrent()
        assertTrue(model.uiState.value.budgetSaveFailed)
        assertEquals("12000000", model.uiState.value.budgetInput)
        assertEquals(0, model.uiState.value.budgetSaveVersion)
        budgets.failWrite = false; budgets.writeGate = CompletableDeferred()
        model.saveMonthlyBudget(); model.saveMonthlyBudget(); runCurrent()
        assertEquals(2, budgets.writes)
        assertTrue(model.uiState.value.isSavingBudget)
        budgets.writeGate!!.complete(Unit); runCurrent()
        assertEquals(12_000_000L, model.uiState.value.budgetLimitVnd)
        assertEquals(1, model.uiState.value.budgetSaveVersion)
        model.clearMonthlyBudget(); runCurrent()
        assertNull(model.uiState.value.budgetLimitVnd)
    }

    @Test fun loadFailureAndRetryRetainMonthAndReplaceBudgetCollector() = runTest(dispatcher) {
        budgets.failObservation = true
        val model = model(); runCurrent()
        assertEquals(DashboardLoadStatus.ERROR, model.uiState.value.budgetLoadStatus)
        model.changeMonth(-1)
        budgets.failObservation = false
        model.retryBudgets(); runCurrent()
        assertEquals(month.minusMonths(1), model.uiState.value.selectedMonth)
        assertEquals(DashboardLoadStatus.READY, model.uiState.value.budgetLoadStatus)
        repeat(3) { model.retryBudgets(); runCurrent() }
        assertEquals(1, budgets.maxActive)
        store.clear(); runCurrent()
        assertEquals(0, budgets.active)
    }

    @Test fun dateMovesAndOverflowNeverLeaveBudgetValuesFromAnotherSnapshot() = runTest(dispatcher) {
        budgets.limits.value = mapOf(month to 100L, month.plusMonths(1) to 300L)
        records.value = listOf(Transaction(1, TransactionType.EXPENSE, 150, "Food", month.atDay(1), null))
        val model = model(); runCurrent()
        records.value = listOf(records.value.single().copy(date = month.plusMonths(1).atDay(1)))
        runCurrent()
        assertEquals(0L, model.uiState.value.budget?.spentVnd)
        model.changeMonth(1)
        assertEquals(150L, model.uiState.value.budget?.remainingVnd)
        records.value = records.value + records.value.single().copy(id = 2, amountVnd = Long.MAX_VALUE)
        runCurrent()
        assertEquals(DashboardError.CALCULATION_OVERFLOW, model.uiState.value.error)
        assertNull(model.uiState.value.summary)
        assertNull(model.uiState.value.budget)
        records.value = listOf(records.value.first().copy(amountVnd = 50))
        runCurrent()
        assertEquals(250L, model.uiState.value.budget?.remainingVnd)
    }

    @Test fun failedTransactionObservationRetainsOnlyClearlyStaleBudgetThenRetryRecovers() = runTest(dispatcher) {
        budgets.limits.value = mapOf(month to 100L)
        records.value = listOf(Transaction(1, TransactionType.EXPENSE, 50, "Food", month.atDay(1), null))
        val model = model(); runCurrent()
        failSource = true
        records.value = listOf(records.value.single().copy(amountVnd = 150))
        runCurrent()
        assertTrue(model.uiState.value.isStale)
        assertEquals(50L, model.uiState.value.budget?.spentVnd)
        failSource = false
        model.retryObservation(); runCurrent()
        assertFalse(model.uiState.value.isStale)
        assertEquals(150L, model.uiState.value.budget?.spentVnd)
    }
}
