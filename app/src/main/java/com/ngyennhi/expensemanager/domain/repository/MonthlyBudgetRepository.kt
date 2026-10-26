package com.ngyennhi.expensemanager.domain.repository

import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface MonthlyBudgetRepository {
    fun observeLimits(): Flow<Map<YearMonth, Long>>
    suspend fun setLimit(month: YearMonth, amountVnd: Long?)
}
