package com.ngyennhi.expensemanager.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.domain.usecase.*
import com.ngyennhi.expensemanager.presentation.home.*
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
class HomeBudgetTest {
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
    private fun model(handle: SavedStateHandle = SavedStateHandle(mapOf("dashboard_selected_month" to month.toString()))) =
        HomeViewModel(ObserveTransactionsUseCase(transactions), CalculateMonthlySummaryUseCase(),
        handle, ObserveMonthlyBudgetsUseCase(budgets),
        SaveMonthlyBudgetUseCase(budgets), CalculateMonthlyBudgetUseCase()).also { store.put("home", it) }

    @Test fun loadingUnsetAndConfiguredLimitsAreDistinctAndReactToExpenseUpdates() = runTest(dispatcher) {
        budgets.gate = CompletableDeferred()
        val model = model()
        runCurrent()
        assertEquals(HomeLoadStatus.LOADING, model.uiState.value.budgetLoadStatus)
        assertNull(model.uiState.value.budget)
        budgets.gate!!.complete(Unit)
        runCurrent()
        assertEquals(HomeLoadStatus.READY, model.uiState.value.budgetLoadStatus)
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
        assertEquals(listOf(HomeCategory("Food", TransactionType.INCOME)), model.uiState.value.categories)
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
        assertEquals(HomeLoadStatus.ERROR, model.uiState.value.budgetLoadStatus)
        model.changeMonth(-1)
        budgets.failObservation = false
        model.retryBudgets(); runCurrent()
        assertEquals(month.minusMonths(1), model.uiState.value.selectedMonth)
        assertEquals(HomeLoadStatus.READY, model.uiState.value.budgetLoadStatus)
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
        assertEquals(HomeError.CALCULATION_OVERFLOW, model.uiState.value.error)
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

    @Test fun editingReplacesOnlySelectedMonthAndRemovingDoesNotTouchOtherMonths() = runTest(dispatcher) {
        budgets.limits.value = mapOf(month to 100L, month.plusMonths(1) to 300L)
        val model = model(); runCurrent()
        model.startBudgetEdit()
        assertEquals("100", model.uiState.value.budgetInput)
        model.setBudgetInput("200"); model.saveMonthlyBudget(); runCurrent()
        assertEquals(200L, model.uiState.value.budget?.remainingVnd)
        assertEquals(mapOf(month to 200L, month.plusMonths(1) to 300L), budgets.limits.value)
        model.clearMonthlyBudget(); runCurrent()
        assertEquals(mapOf(month.plusMonths(1) to 300L), budgets.limits.value)
        assertNull(model.uiState.value.budget)
    }

    @Test fun switchingMonthsDiscardsDraftErrorsAndReopeningRestoresSavedLimitWithoutWriting() = runTest(dispatcher) {
        budgets.limits.value = mapOf(month to 100L, month.plusMonths(1) to 300L)
        val model = model(); runCurrent()
        model.startBudgetEdit(); model.setBudgetInput("bad"); model.saveMonthlyBudget()
        assertTrue(model.uiState.value.budgetInputInvalid)
        model.changeMonth(1)
        assertEquals("300", model.uiState.value.budgetInput)
        assertFalse(model.uiState.value.budgetInputInvalid)
        model.setBudgetInput("999")
        model.startBudgetEdit() // Reopening after dismiss/cancel discards unsaved edits.
        assertEquals("300", model.uiState.value.budgetInput)
        model.changeMonth(1)
        assertEquals("", model.uiState.value.budgetInput)
        model.changeMonth(-2)
        assertEquals("100", model.uiState.value.budgetInput)
        assertEquals(0, budgets.writes)
    }

    @Test fun delayedLimitsPrefillEditorWithoutOverwritingAUserDraft() = runTest(dispatcher) {
        budgets.gate = CompletableDeferred()
        budgets.limits.value = mapOf(month to 100L)
        val model = model(); runCurrent()
        model.startBudgetEdit()
        budgets.gate!!.complete(Unit); runCurrent()
        assertEquals("100", model.uiState.value.budgetInput)
        model.setBudgetInput("250")
        budgets.limits.value = mapOf(month to 150L); runCurrent()
        assertEquals("250", model.uiState.value.budgetInput)
        assertEquals(150L, model.uiState.value.budgetLimitVnd)
        model.startBudgetEdit()
        assertEquals("150", model.uiState.value.budgetInput)
    }

    @Test fun inFlightWriteBlocksMonthAndDraftChangesAndWritesOriginalMonthOnce() = runTest(dispatcher) {
        budgets.writeGate = CompletableDeferred()
        val model = model(); runCurrent()
        model.startBudgetEdit(); model.setBudgetInput("200"); model.saveMonthlyBudget(); runCurrent()
        model.changeMonth(1); model.setBudgetInput("0"); model.startBudgetEdit()
        model.saveMonthlyBudget(); model.clearMonthlyBudget(); runCurrent()
        assertEquals(month, model.uiState.value.selectedMonth)
        assertEquals("200", model.uiState.value.budgetInput)
        assertFalse(model.uiState.value.budgetInputInvalid)
        assertEquals(1, budgets.writes)
        budgets.writeGate!!.complete(Unit); runCurrent()
        assertEquals(mapOf(month to 200L), budgets.limits.value)
        assertFalse(model.uiState.value.isSavingBudget)
        model.changeMonth(1)
        assertEquals("", model.uiState.value.budgetInput)
        assertNull(model.uiState.value.budget)
    }

    @Test fun failedRemovalCanRetryRemovalAndNeverReportsSuccessOrChangesAnotherMonth() = runTest(dispatcher) {
        budgets.limits.value = mapOf(month to 100L, month.plusMonths(1) to 300L)
        val model = model(); runCurrent()
        model.startBudgetEdit(); budgets.failWrite = true
        model.clearMonthlyBudget(); runCurrent()
        assertTrue(model.uiState.value.budgetSaveFailed)
        assertEquals(100L, model.uiState.value.budgetLimitVnd)
        assertEquals(0, model.uiState.value.budgetSaveVersion)
        budgets.failWrite = false
        model.clearMonthlyBudget(); runCurrent()
        assertFalse(model.uiState.value.budgetSaveFailed)
        assertEquals(1, model.uiState.value.budgetSaveVersion)
        assertEquals(mapOf(month.plusMonths(1) to 300L), budgets.limits.value)
    }

    @Test fun retryFailureInvalidatesCachedLimitsAndRecoversWithFreshMonthValues() = runTest(dispatcher) {
        budgets.limits.value = mapOf(month to 100L)
        val model = model(); runCurrent()
        budgets.failObservation = true
        model.retryBudgets(); runCurrent()
        assertEquals(HomeLoadStatus.ERROR, model.uiState.value.budgetLoadStatus)
        assertNull(model.uiState.value.budgetLimitVnd)
        model.changeMonth(1)
        budgets.limits.value = mapOf(month.plusMonths(1) to 500L)
        budgets.failObservation = false
        model.retryBudgets(); runCurrent()
        assertEquals("500", model.uiState.value.budgetInput)
        assertEquals(500L, model.uiState.value.budget?.remainingVnd)
        assertEquals(1, budgets.maxActive)
    }

    @Test fun restoredMonthUsesItsOwnLimitAndSpendingAcrossYearBoundary() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("dashboard_selected_month" to "2026-12"))
        val december = YearMonth.of(2026, 12)
        val january = december.plusMonths(1)
        budgets.limits.value = mapOf(december to 100L, january to 300L)
        records.value = listOf(Transaction(1, TransactionType.EXPENSE, 150, "Food", january.atDay(1), null))
        val original = model(handle); runCurrent()
        original.changeMonth(1)
        val savedMonth = handle.get<String>("dashboard_selected_month")
        assertEquals("2027-01", savedMonth)
        store.clear(); runCurrent()
        val restored = model(SavedStateHandle(mapOf("dashboard_selected_month" to savedMonth)))
        runCurrent()
        assertEquals(january, restored.uiState.value.selectedMonth)
        assertEquals(150L, restored.uiState.value.budget?.remainingVnd)
        assertEquals("300", restored.uiState.value.budgetInput)
        assertEquals(1, budgets.maxActive)
    }
}
