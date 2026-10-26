package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateMonthlyStatisticsUseCaseTest {
    private val calculate = CalculateMonthlyStatisticsUseCase(CalculateMonthlySummaryUseCase())
    private val month = YearMonth.of(2026, 1)

    @Test
    fun emptyAndIncomeOnlyMonthsHaveNoSlicesAndSixZeroFilledMonths() {
        val empty = calculate(emptyList(), month)
        assertEquals(0L, empty.summary.incomeVnd)
        assertEquals(0L, empty.summary.expenseVnd)
        assertTrue(empty.categories.isEmpty())
        assertEquals((0L..5L).map { YearMonth.of(2025, 8).plusMonths(it) }, empty.comparison.map { it.month })
        assertTrue(empty.comparison.all { it.incomeVnd == 0L && it.expenseVnd == 0L })

        val income = calculate(listOf(record(100, TransactionType.INCOME)), month)
        assertEquals(100L, income.summary.incomeVnd)
        assertTrue(income.categories.isEmpty())
    }

    @Test
    fun groupsOnlySelectedMonthExpensesAndSortsTiesByStoredName() {
        val result = calculate(listOf(
            record(20, category = "Food"), record(20, category = "Food"),
            record(30, category = "B"), record(30, category = "A"),
            record(999, TransactionType.INCOME, "Food"),
            record(99, date = month.minusMonths(1).atEndOfMonth()),
            record(88, date = month.plusMonths(1).atDay(1)),
        ), month)
        assertEquals(999L, result.summary.incomeVnd)
        assertEquals(100L, result.summary.expenseVnd)
        assertEquals(listOf("Food", "A", "B"), result.categories.map { it.category })
        assertEquals(listOf(40L, 30L, 30L), result.categories.map { it.amountVnd })
        assertEquals(0.4, result.categories.first().share, 0.00000001)
        assertEquals(1.0, result.categories.sumOf { it.share }, 0.00000001)
        assertEquals(99L, result.comparison[4].expenseVnd)
    }

    @Test
    fun comparisonIncludesLeapDayAndBothRangeEdgesButExcludesOutsideRecords() {
        val selected = YearMonth.of(2024, 7)
        val result = calculate(listOf(
            record(1, date = LocalDate.of(2024, 1, 31)),
            record(2, date = LocalDate.of(2024, 2, 1)),
            record(3, TransactionType.INCOME, date = LocalDate.of(2024, 2, 29)),
            record(4, date = LocalDate.of(2024, 7, 31)),
            record(5, date = LocalDate.of(2024, 8, 1)),
        ), selected)
        assertEquals(YearMonth.of(2024, 2), result.comparison.first().month)
        assertEquals(2L, result.comparison.first().expenseVnd)
        assertEquals(3L, result.comparison.first().incomeVnd)
        assertEquals(4L, result.summary.expenseVnd)
        assertEquals(6L, result.comparison.sumOf { it.expenseVnd })
    }

    @Test
    fun largeAmountsAndUnevenFractionsRemainFiniteWithoutMultiplicationOverflow() {
        val result = calculate(listOf(record(Long.MAX_VALUE - 1, category = "A"), record(1, category = "B")), month)
        assertEquals(Long.MAX_VALUE, result.summary.expenseVnd)
        assertTrue(result.categories.all { it.share.isFinite() && it.share > 0.0 && it.share <= 1.0 })
        val thirds = calculate(listOf(record(1, category = "A"), record(1, category = "B"), record(1, category = "C")), month)
        assertEquals(1.0 / 3, thirds.categories.first().share, 0.00000001)
    }

    @Test(expected = ArithmeticException::class)
    fun selectedMonthOverflowIsReported() {
        calculate(listOf(record(Long.MAX_VALUE), record(1)), month)
    }

    @Test(expected = ArithmeticException::class)
    fun comparisonMonthOverflowIsReported() {
        calculate(listOf(record(Long.MAX_VALUE, TransactionType.INCOME, date = month.minusMonths(5).atDay(1)),
            record(1, TransactionType.INCOME, date = month.minusMonths(5).atDay(2))), month)
    }

    private fun record(amount: Long, type: TransactionType = TransactionType.EXPENSE,
        category: String = "Food", date: LocalDate = month.atDay(1)) =
        Transaction(1, type, amount, category, date, null)
}
