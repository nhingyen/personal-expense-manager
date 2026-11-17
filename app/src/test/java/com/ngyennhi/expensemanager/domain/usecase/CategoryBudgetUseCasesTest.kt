package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.CategoryBudgetLimit
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetRepository
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetWriteResult
import java.math.BigInteger
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CategoryBudgetUseCasesTest {
    private val month = YearMonth.of(2026, 11)
    private val food = Category(1, TransactionType.EXPENSE, "Food", false)
    private val travel = Category(2, TransactionType.EXPENSE, "Đi lại", false)
    private val salary = Category(3, TransactionType.INCOME, "Salary", false)
    private val calculate = CalculateCategoryBudgetsUseCase()

    @Test fun calculatesSelectedMonthExpensesAndLegacyVariantsExactly() {
        val result = calculate(
            categories = listOf(food, travel, salary),
            transactions = listOf(
                transaction(1, TransactionType.EXPENSE, 40, " Food ", month.minusMonths(1)),
                transaction(2, TransactionType.INCOME, 500, "Salary", month),
                transaction(3, TransactionType.EXPENSE, 60, "fOoD", month),
                transaction(4, TransactionType.EXPENSE, 90, "FOOD", month),
                transaction(5, TransactionType.EXPENSE, 25, "đI LạI", month),
            ),
            limits = listOf(
                CategoryBudgetLimit(food.id, month, 100),
                CategoryBudgetLimit(travel.id, month, 100),
                CategoryBudgetLimit(food.id, month.plusMonths(1), 1),
            ),
            month = month,
        )

        assertEquals(listOf(food.id, travel.id), result.map { it.categoryId })
        assertEquals(listOf(150L, 25L), result.map { it.spentVnd })
        assertEquals(-50L, result[0].remainingVnd)
        assertEquals(BigInteger.valueOf(150), result[0].usedPercent)
        assertTrue(result[0].limitReached)
        assertFalse(result[1].limitReached)
    }

    @Test fun archivedCategoryStillCalculatesItsPreservedLimit() {
        val archived = food.copy(isArchived = true)
        val result = calculate(
            categories = listOf(archived),
            transactions = listOf(transaction(1, TransactionType.EXPENSE, 100, "food", month)),
            limits = listOf(CategoryBudgetLimit(food.id, month, 100)),
            month = month,
        ).single()

        assertEquals(100L, result.spentVnd)
        assertTrue(result.limitReached)
    }

    @Test fun checkedSpendingSumReportsLongOverflow() {
        assertThrows(ArithmeticException::class.java) {
            calculate(
                categories = listOf(food),
                transactions = listOf(
                    transaction(1, TransactionType.EXPENSE, Long.MAX_VALUE, "Food", month),
                    transaction(2, TransactionType.EXPENSE, 1, "food", month),
                ),
                limits = listOf(CategoryBudgetLimit(food.id, month, 1)),
                month = month,
            )
        }
    }

    @Test fun inconsistentSnapshotsNeverBecomeZeroSpending() {
        assertThrows(InconsistentCategoryBudgetSnapshotException::class.java) {
            calculate(
                categories = listOf(food.copy(name = "Dining")),
                transactions = listOf(transaction(1, TransactionType.EXPENSE, 50, "Food", month)),
                limits = listOf(CategoryBudgetLimit(food.id, month, 100)),
                month = month,
            )
        }
        assertThrows(InconsistentCategoryBudgetSnapshotException::class.java) {
            calculate(
                categories = emptyList(),
                transactions = emptyList(),
                limits = listOf(CategoryBudgetLimit(food.id, month, 100)),
                month = month,
            )
        }
    }

    @Test fun saveUseCaseValidatesBeforeWriting() = runTest {
        val repository = FakeCategoryBudgetRepository()
        val save = SaveCategoryBudgetUseCase(repository)

        for (invalid in listOf(0L, -1L)) {
            try {
                save(food.id, month, invalid)
                fail("Invalid limit accepted")
            } catch (_: IllegalArgumentException) { }
        }
        assertEquals(0, repository.writes)

        assertEquals(CategoryBudgetWriteResult.SAVED, save(food.id, month, Long.MAX_VALUE))
        assertEquals(CategoryBudgetWriteResult.REMOVED, save(food.id, month, null))
        assertEquals(2, repository.writes)
    }

    private fun transaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        category: String,
        transactionMonth: YearMonth,
    ) = Transaction(id, type, amount, category, transactionMonth.atDay(1), null)
}

private class FakeCategoryBudgetRepository : CategoryBudgetRepository {
    private val limits = MutableStateFlow<List<CategoryBudgetLimit>>(emptyList())
    var writes = 0

    override fun observeLimits() = limits

    override suspend fun setLimit(categoryId: Long, month: YearMonth, amountVnd: Long?): CategoryBudgetWriteResult {
        writes++
        limits.value = limits.value.filterNot { it.categoryId == categoryId && it.month == month } +
            listOfNotNull(amountVnd?.let { CategoryBudgetLimit(categoryId, month, it) })
        return if (amountVnd == null) CategoryBudgetWriteResult.REMOVED else CategoryBudgetWriteResult.SAVED
    }
}
