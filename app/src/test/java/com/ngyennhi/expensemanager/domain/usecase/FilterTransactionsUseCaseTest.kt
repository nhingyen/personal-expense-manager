package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDateRange
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterTransactionsUseCaseTest {
    private val filter = FilterTransactionsUseCase()
    private val period = TransactionDateRange(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 2))
    private val records = listOf(
        record(4, "2026-10-03", "Food", "Lunch"),
        record(3, "2026-10-02", "Ăn uống", "Cà phê sáng"),
        record(2, "2026-10-01", "Food", null, TransactionType.INCOME),
        record(1, "2026-09-30", "Food", "Lunch"),
        record(0, "2026-09-29", "Food", "Lunch"),
    )

    @Test fun searchMatchesNoteOrCategoryIgnoringCaseAndQueryWhitespace() {
        assertEquals(listOf(3L), ids(query = "  CÀ PHÊ  "))
        assertEquals(listOf(3L), ids(query = "UỐNG"))
        assertEquals(listOf(2L, 1L), ids(query = "fOoD"))
        assertEquals(listOf(3L, 2L, 1L), ids(query = " \t "))
        assertTrue(ids(query = "ca phe").isEmpty()) // Accents remain significant.
        assertTrue(ids(query = "missing").isEmpty())
    }

    @Test fun allConditionsCombineWithAndAndCategoryUsesExactMatch() {
        assertEquals(listOf(1L), filter(records, period, "LUNCH", TransactionType.EXPENSE, " food ").map { it.id })
        assertEquals(listOf(2L), filter(records, period, type = TransactionType.INCOME, category = "Food").map { it.id })
        assertTrue(filter(records, period, category = "Foo").isEmpty())
        assertTrue(filter(records, period, "Lunch", TransactionType.INCOME, "Food").isEmpty())
    }

    @Test fun inclusiveCrossMonthRangePreservesRepositoryOrderingAndInput() {
        val before = records.toList()
        assertEquals(listOf(3L, 2L, 1L), ids())
        assertEquals(before, records)
        val oneDay = TransactionDateRange(period.start, period.start)
        assertEquals(listOf(1L), filter(records, oneDay).map { it.id })
        assertTrue(filter(emptyList(), period).isEmpty())
    }

    @Test fun yearAndLeapDayBoundariesUseLocalDates() {
        val dates = listOf("2024-03-01", "2024-02-29", "2024-02-28", "2023-12-31", "2023-12-30")
        val values = dates.mapIndexed { index, date -> record(index.toLong(), date, "Food", null) }
        val range = TransactionDateRange(LocalDate.of(2023, 12, 31), LocalDate.of(2024, 2, 29))
        assertEquals(listOf(1L, 2L, 3L), filter(values, range).map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun reversedRangeIsRejected() {
        TransactionDateRange(period.endInclusive, period.start)
    }

    @Test fun searchDoesNotDependOnDeviceLocaleAndTreatsWildcardsLiterally() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val values = listOf(record(1, "2026-10-01", "INCOME", "100%_saved"))
            assertEquals(values, filter(values, period, "income"))
            assertEquals(values, filter(values, period, "%_"))
            assertTrue(filter(values, period, "%missing").isEmpty())
        } finally {
            Locale.setDefault(previous)
        }
    }

    private fun ids(query: String = "") = filter(records, period, query).map { it.id }

    private fun record(id: Long, date: String, category: String, note: String?,
        type: TransactionType = TransactionType.EXPENSE) =
        Transaction(id, type, 100, category, LocalDate.parse(date), note)
}
