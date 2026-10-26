package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.MonthlyBudgetRepository
import java.math.BigInteger
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class MonthlyBudgetUseCasesTest {
    private val month = YearMonth.of(2026, 10)
    private val calculate = CalculateMonthlyBudgetUseCase()
    private fun summary(expense: Long) = MonthlySummary(month, 0, expense, -expense)

    @Test fun unsetLimitDoesNotInventAZeroBudget() { assertNull(calculate(summary(100), null)) }
    @Test fun matchesFigmaExampleUsingActualLimitAndExpense() {
        val result = calculate(summary(8_200_000), 12_000_000)!!
        assertEquals(3_800_000L, result.remainingVnd)
        assertEquals(BigInteger.valueOf(68), result.usedPercent)
        assertFalse(result.limitReached)
    }
    @Test fun emptyReachedAndExceededBudgetsHaveExactValues() {
        val empty = calculate(summary(0), 100)!!
        assertEquals(BigInteger.ZERO, empty.usedPercent)
        assertEquals(100L, empty.remainingVnd)
        assertTrue(calculate(summary(100), 100)!!.limitReached)
        val exceeded = calculate(summary(150), 100)!!
        assertEquals(-50L, exceeded.remainingVnd)
        assertEquals(BigInteger.valueOf(150), exceeded.usedPercent)
    }
    @Test fun percentageCannotOverflowEvenAtLongBoundary() {
        val result = calculate(summary(Long.MAX_VALUE), 1)!!
        assertEquals(Long.MAX_VALUE.toBigInteger() * BigInteger.valueOf(100), result.usedPercent)
        assertEquals(1 - Long.MAX_VALUE, result.remainingVnd)
    }
    @Test(expected = IllegalArgumentException::class) fun zeroLimitIsRejected() { calculate(summary(0), 0) }
    @Test fun spendingUsesOnlySelectedMonthExpensesAcrossYearBoundary() {
        val selected = YearMonth.of(2027, 1)
        val records = listOf(
            Transaction(1, TransactionType.EXPENSE, 500, "Food", selected.minusMonths(1).atEndOfMonth(), null),
            Transaction(2, TransactionType.INCOME, 900, "Salary", selected.atDay(1), null),
            Transaction(3, TransactionType.EXPENSE, 50, "Food", selected.atDay(1), null),
            Transaction(4, TransactionType.EXPENSE, 75, "Food", selected.atEndOfMonth(), null),
            Transaction(5, TransactionType.EXPENSE, 500, "Food", selected.plusMonths(1).atDay(1), null),
        )
        val result = calculate(CalculateMonthlySummaryUseCase()(records, selected), 100)!!
        assertEquals(selected, result.month)
        assertEquals(125L, result.spentVnd)
        assertEquals(-25L, result.remainingVnd)
        assertEquals(BigInteger.valueOf(125), result.usedPercent)
        assertTrue(result.limitReached)
    }

    @Test fun largestLimitHasExactRemainingAndReachedBoundary() {
        val below = calculate(summary(Long.MAX_VALUE - 1), Long.MAX_VALUE)!!
        assertEquals(1L, below.remainingVnd)
        assertEquals(BigInteger.valueOf(99), below.usedPercent)
        assertFalse(below.limitReached)
        val reached = calculate(summary(Long.MAX_VALUE), Long.MAX_VALUE)!!
        assertEquals(0L, reached.remainingVnd)
        assertEquals(BigInteger.valueOf(100), reached.usedPercent)
        assertTrue(reached.limitReached)
    }
    @Test fun saveValidatesLimitsAndSeparatesMonthsAndClearing() = runTest {
        val repository = object : MonthlyBudgetRepository {
            val limits = MutableStateFlow<Map<YearMonth, Long>>(emptyMap())
            var writes = 0
            override fun observeLimits() = limits
            override suspend fun setLimit(month: YearMonth, amountVnd: Long?) {
                writes++
                limits.value = if (amountVnd == null) limits.value - month else limits.value + (month to amountVnd)
            }
        }
        val save = SaveMonthlyBudgetUseCase(repository)
        for (invalid in listOf(0L, -1L)) {
            try { save(month, invalid); fail("Invalid limit accepted") } catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, repository.writes)
        save(month, Long.MAX_VALUE)
        save(month.plusMonths(1), 100)
        save(month, null)
        assertEquals(mapOf(month.plusMonths(1) to 100L), repository.limits.value)
    }
}
