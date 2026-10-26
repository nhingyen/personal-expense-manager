package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.CategoryExpense
import com.ngyennhi.expensemanager.domain.model.MonthlyStatistics
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.YearMonth

class CalculateMonthlyStatisticsUseCase(
    private val calculateSummary: CalculateMonthlySummaryUseCase,
) {
    operator fun invoke(transactions: List<Transaction>, month: YearMonth): MonthlyStatistics {
        val comparison = (5L downTo 0L).map { offset ->
            calculateSummary(transactions, month.minusMonths(offset))
        }
        val summary = comparison.last()
        val amounts = mutableMapOf<String, Long>()
        transactions.forEach { transaction ->
            if (transaction.type == TransactionType.EXPENSE && YearMonth.from(transaction.date) == month) {
                amounts[transaction.category] = Math.addExact(amounts[transaction.category] ?: 0L, transaction.amountVnd)
            }
        }
        val categories = amounts.map { (category, amount) ->
            CategoryExpense(category, amount,
                if (summary.expenseVnd == 0L) 0.0 else amount.toDouble() / summary.expenseVnd.toDouble())
        }.sortedWith(compareByDescending<CategoryExpense> { it.amountVnd }.thenBy { it.category })
        return MonthlyStatistics(summary, categories, comparison)
    }
}
