
package com.ngyennhi.expensemanager.presentation.dashboard

import androidx.lifecycle.SavedStateHandle
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlySummaryUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveMonthlyBudgetsUseCase
import com.ngyennhi.expensemanager.domain.usecase.SaveMonthlyBudgetUseCase
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlyBudgetUseCase
import com.ngyennhi.expensemanager.presentation.FakeMonthlyBudgetRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val budgets = FakeMonthlyBudgetRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        repository: FakeTransactionRepository
    ): DashboardViewModel {

        val savedStateHandle = SavedStateHandle(
            mapOf("dashboard_selected_month" to "2026-10")
        )

        return DashboardViewModel(
            observeTransactions = ObserveTransactionsUseCase(repository),
            calculateMonthlySummary = CalculateMonthlySummaryUseCase(),
            savedStateHandle = savedStateHandle,
            observeBudgets = ObserveMonthlyBudgetsUseCase(budgets),
            saveBudget = SaveMonthlyBudgetUseCase(budgets),
            calculateBudget = CalculateMonthlyBudgetUseCase(),
        )
    }

    // TEST 1: Tháng không có giao dịch
    @Test
    fun emptyMonthReturnsZero() = runTest(dispatcher) {

        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        // Trước khi Flow được xử lý
        assertEquals(
            DashboardLoadStatus.LOADING,
            viewModel.uiState.value.loadStatus
        )

        runCurrent()

        val state = viewModel.uiState.value

        assertEquals(DashboardLoadStatus.READY, state.loadStatus)
        assertEquals(0L, state.summary?.incomeVnd)
        assertEquals(0L, state.summary?.expenseVnd)
        assertEquals(0L, state.summary?.netVnd)
        assertTrue(state.isEmpty)
    }

    // TEST 2: Tính thu chi tháng 10
    @Test
    fun calculatesMonthlyIncomeAndExpense() = runTest(dispatcher) {

        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        runCurrent()

        repository.transactions.value = listOf(
            Transaction(
                id = 1L,
                type = TransactionType.INCOME,
                amountVnd = 20_000_000L,
                category = "Lương",
                date = LocalDate.of(2026, 10, 1),
                note = null
            ),
            Transaction(
                id = 2L,
                type = TransactionType.EXPENSE,
                amountVnd = 8_200_000L,
                category = "Sinh hoạt",
                date = LocalDate.of(2026, 10, 20),
                note = null
            ),
            Transaction(
                id = 3L,
                type = TransactionType.EXPENSE,
                amountVnd = 500_000L,
                category = "Mua sắm",
                date = LocalDate.of(2026, 9, 30),
                note = null
            )
        )

        runCurrent()

        val state = viewModel.uiState.value

        assertEquals(20_000_000L, state.summary?.incomeVnd)
        assertEquals(8_200_000L, state.summary?.expenseVnd)
        assertEquals(11_800_000L, state.summary?.netVnd)

        // Chỉ hai giao dịch thuộc tháng 10
        assertEquals(2, state.recentTransactions.size)

        // Ngày mới nhất đứng trước
        assertEquals(2L, state.recentTransactions.first().id)
    }

    // TEST 3: Đổi tháng
    @Test
    fun changingMonthRecalculatesSummary() = runTest(dispatcher) {

        val repository = FakeTransactionRepository()
        val viewModel = createViewModel(repository)

        runCurrent()

        repository.transactions.value = listOf(
            Transaction(
                id = 1L,
                type = TransactionType.INCOME,
                amountVnd = 1_000_000L,
                category = "Lương",
                date = LocalDate.of(2026, 10, 1),
                note = null
            ),
            Transaction(
                id = 2L,
                type = TransactionType.EXPENSE,
                amountVnd = 200_000L,
                category = "Ăn uống",
                date = LocalDate.of(2026, 9, 10),
                note = null
            )
        )

        runCurrent()

        // Ban đầu đang xem tháng 10
        assertEquals(
            1_000_000L,
            viewModel.uiState.value.summary?.incomeVnd
        )

        // Chuyển về tháng 9
        viewModel.changeMonth(-1)

        val state = viewModel.uiState.value

        assertEquals(
            YearMonth.of(2026, 9),
            state.selectedMonth
        )
        assertEquals(0L, state.summary?.incomeVnd)
        assertEquals(200_000L, state.summary?.expenseVnd)
        assertEquals(-200_000L, state.summary?.netVnd)
    }
}

// Repository giả lập cho các bài test
private class FakeTransactionRepository : TransactionRepository {

    val transactions = MutableStateFlow<List<Transaction>>(
        emptyList()
    )

    override fun observeTransactions(): Flow<List<Transaction>> {
        return transactions
    }

    override suspend fun add(
        transaction: TransactionDraft
    ): Long {
        error("Not used in Dashboard tests")
    }

    override suspend fun update(
        transaction: Transaction
    ): Boolean {
        error("Not used in Dashboard tests")
    }

    override suspend fun delete(id: Long): Boolean {
        error("Not used in Dashboard tests")
    }
}
