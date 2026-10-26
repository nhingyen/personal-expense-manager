package com.ngyennhi.expensemanager.presentation.statistics

import com.ngyennhi.expensemanager.domain.model.MonthlyStatistics
import java.time.YearMonth

enum class StatisticsError { LOAD_FAILED, CALCULATION_OVERFLOW }

data class StatisticsUiState(
    val selectedMonth: YearMonth,
    val isLoading: Boolean = true,
    val statistics: MonthlyStatistics? = null,
    val error: StatisticsError? = null,
)
