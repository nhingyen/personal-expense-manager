package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthlySummaryBoundaryTest {
    private val calculate = CalculateMonthlySummaryUseCase()
    private val october = YearMonth.of(2026, 10)

    @Test
    fun includesFirstAndLastDayButExcludesAdjacentMonthsAndOtherYears() {
        val result = calculate(listOf(
            record("2026-10-01", 100), record("2026-10-31", 200),
            record("2026-09-30", 900), record("2026-11-01", 900),
            record("2025-10-01", 900), record("2027-10-31", 900),
        ), october)
        assertEquals(october, result.month)
        assertEquals(300L, result.incomeVnd)
    }

    @Test
    fun decemberJanuaryAndLeapDayUseStoredCalendarDates() {
        val records = listOf(record("2023-12-31", 10), record("2024-01-01", 20),
            record("2024-02-29", 30), record("2024-03-01", 40))
        assertEquals(10L, calculate(records, YearMonth.of(2023, 12)).netVnd)
        assertEquals(20L, calculate(records, YearMonth.of(2024, 1)).netVnd)
        assertEquals(30L, calculate(records, YearMonth.of(2024, 2)).netVnd)
    }

    @Test
    fun incomeOnlyExpenseOnlyAndNegativeNetAreValid() {
        val income = record("2026-10-01", 100_000)
        val expense = record("2026-10-31", 150_000, TransactionType.EXPENSE)
        assertEquals(100_000L, calculate(listOf(income), october).netVnd)
        assertEquals(-150_000L, calculate(listOf(expense), october).netVnd)
        assertEquals(-50_000L, calculate(listOf(income, expense), october).netVnd)
    }

    @Test
    fun zeroNetWithRecordsIsValid() {
        val income = record("2026-10-01", 100_000)
        val result = calculate(listOf(income, income.copy(type = TransactionType.EXPENSE)), october)
        assertEquals(100_000L, result.incomeVnd)
        assertEquals(100_000L, result.expenseVnd)
        assertEquals(0L, result.netVnd)
    }

    @Test
    fun totalsStayExactBeyondIntAndAtLongBoundary() {
        val income = record("2026-10-01", Long.MAX_VALUE - 1)
        val result = calculate(listOf(income, income.copy(amountVnd = 1)), october)
        assertEquals(Long.MAX_VALUE, result.incomeVnd)
        assertEquals(Long.MAX_VALUE, result.netVnd)
        val expense = income.copy(type = TransactionType.EXPENSE, amountVnd = Long.MAX_VALUE)
        assertEquals(-Long.MAX_VALUE, calculate(listOf(expense), october).netVnd)
        assertEquals(0L, calculate(listOf(income.copy(amountVnd = Long.MAX_VALUE), expense), october).netVnd)
        assertEquals(4_000_000_000L, calculate(listOf(income.copy(amountVnd = 4_000_000_000L)), october).netVnd)
    }

    @Test(expected = ArithmeticException::class)
    fun incomeOverflowIsReportedEvenWhenNetWouldFit() {
        calculate(listOf(record("2026-10-01", Long.MAX_VALUE), record("2026-10-02", 1),
            record("2026-10-03", Long.MAX_VALUE, TransactionType.EXPENSE)), october)
    }

    @Test(expected = ArithmeticException::class)
    fun expenseOverflowIsReported() {
        calculate(listOf(record("2026-10-01", Long.MAX_VALUE, TransactionType.EXPENSE),
            record("2026-10-02", 1, TransactionType.EXPENSE)), october)
    }

    @Test
    fun overflowingAnotherMonthDoesNotAffectSelectedMonth() {
        val result = calculate(listOf(record("2026-09-01", Long.MAX_VALUE),
            record("2026-09-02", 1)), october)
        assertEquals(0L, result.netVnd)
    }

    private fun record(date: String, amount: Long, type: TransactionType = TransactionType.INCOME) =
        Transaction(1, type, amount, "User category", LocalDate.parse(date), null)
}
