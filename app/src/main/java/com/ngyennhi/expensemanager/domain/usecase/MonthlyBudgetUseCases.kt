package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.MonthlyBudget
import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.domain.repository.MonthlyBudgetRepository
import java.time.YearMonth

class ObserveMonthlyBudgetsUseCase(private val repository: MonthlyBudgetRepository) {
    operator fun invoke() = repository.observeLimits()
}

class SaveMonthlyBudgetUseCase(private val repository: MonthlyBudgetRepository) {
    suspend operator fun invoke(month: YearMonth, amountVnd: Long?) {
        require(amountVnd == null || amountVnd > 0)
        repository.setLimit(month, amountVnd)
    }
}

class CalculateMonthlyBudgetUseCase {
    operator fun invoke(summary: MonthlySummary, limitVnd: Long?): MonthlyBudget? =
        limitVnd?.let { MonthlyBudget(summary.month, it, summary.expenseVnd) }
}
