package com.ngyennhi.expensemanager.domain.model

data class CategoryExpense(
    val category: String,
    val amountVnd: Long,
    /** Locale-neutral fraction in [0, 1]; rounding belongs to presentation. */
    val share: Double,
)

data class MonthlyStatistics(
    val summary: MonthlySummary,
    val categories: List<CategoryExpense>,
    /** Six chronological months ending in summary.month, including empty months. */
    val comparison: List<MonthlySummary>,
)
