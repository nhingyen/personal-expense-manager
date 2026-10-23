package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalculateMonthlySummaryUseCaseTest {
    private val useCase = CalculateMonthlySummaryUseCase()

    @Test
    fun calculatesIncomeExpenseAndNet() {
        val transactions = listOf(
            Transaction(
                id = 1L,
                type = TransactionType.INCOME,
                amountVnd = 20_000_000L,
                category = "Luong",
                date = LocalDate.of(2026, 10, 1),
                note = "Luong thang 1"
            ),
            Transaction(
                id = 2L,
                type = TransactionType.EXPENSE,
                amountVnd = 8_200_000L,
                category = "Sinh hoat",
                date = LocalDate.of(2026, 10, 20),
                note = null
            )
        )

        val result = useCase(
            transactions = transactions,
            month = YearMonth.of(2026, 10)
        )

        assertEquals(20_000_000L, result.incomeVnd)
        assertEquals(8_200_000L, result.expenseVnd)
        assertEquals(11_800_000L, result.netVnd)
    }

    @Test
    fun returnsZeroWhenMonthHasNoTransactions() {

        val transactions = emptyList<Transaction>()

        val result = useCase(
            transactions = transactions,
            month = YearMonth.of(2026, 10)
        )

        assertEquals(0L, result.incomeVnd)
        assertEquals(0L, result.expenseVnd)
        assertEquals(0L, result.netVnd)
    }
}