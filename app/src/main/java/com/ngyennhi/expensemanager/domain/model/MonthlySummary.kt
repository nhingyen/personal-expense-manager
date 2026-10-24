package com.ngyennhi.expensemanager.domain.model

import java.time.YearMonth

data class MonthlySummary(
    val month: YearMonth,
    val incomeVnd: Long,
    val expenseVnd: Long,
    val netVnd: Long,
)
