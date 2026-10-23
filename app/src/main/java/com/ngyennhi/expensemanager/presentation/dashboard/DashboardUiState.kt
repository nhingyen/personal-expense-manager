package com.ngyennhi.expensemanager.presentation.dashboard

import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.MonthlyBudget
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.YearMonth

enum class DashboardLoadStatus { LOADING, READY, ERROR }

enum class DashboardError { DATA_LOAD_FAILED, CALCULATION_OVERFLOW; }

data class DashboardCategory(val name: String, val type: TransactionType)

data class DashboardUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val loadStatus: DashboardLoadStatus = DashboardLoadStatus.LOADING,
    val summary: MonthlySummary? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val error: DashboardError? = null,
    val isRetrying: Boolean = false,
    val isStale: Boolean = false,
    val greetingDateTime: LocalDateTime = LocalDateTime.now(),
    val categories: List<DashboardCategory> = emptyList(),
    val budgetLoadStatus: DashboardLoadStatus = DashboardLoadStatus.LOADING,
    val budgetLimitVnd: Long? = null,
    val budget: MonthlyBudget? = null,
    val budgetInput: String = "",
    val budgetInputInvalid: Boolean = false,
    val isSavingBudget: Boolean = false,
    val budgetSaveFailed: Boolean = false,
    val budgetSaveVersion: Int = 0,
) {
    val isEmpty: Boolean
        get() = loadStatus == DashboardLoadStatus.READY &&
                summary?.month == selectedMonth &&
                recentTransactions.isEmpty()

    val canShowData: Boolean
        get() = summary?.month == selectedMonth &&
                error != DashboardError.CALCULATION_OVERFLOW
}
