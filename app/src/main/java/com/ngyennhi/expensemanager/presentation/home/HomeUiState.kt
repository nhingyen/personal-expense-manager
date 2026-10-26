package com.ngyennhi.expensemanager.presentation.home

import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.MonthlyBudget
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.YearMonth

enum class HomeLoadStatus { LOADING, READY, ERROR }

enum class HomeError { DATA_LOAD_FAILED, CALCULATION_OVERFLOW; }

data class HomeCategory(val name: String, val type: TransactionType)

data class HomeUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val loadStatus: HomeLoadStatus = HomeLoadStatus.LOADING,
    val summary: MonthlySummary? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val error: HomeError? = null,
    val isRetrying: Boolean = false,
    val isStale: Boolean = false,
    val greetingDateTime: LocalDateTime = LocalDateTime.now(),
    val categories: List<HomeCategory> = emptyList(),
    val budgetLoadStatus: HomeLoadStatus = HomeLoadStatus.LOADING,
    val budgetLimitVnd: Long? = null,
    val budget: MonthlyBudget? = null,
    val budgetInput: String = "",
    val budgetInputInvalid: Boolean = false,
    val isSavingBudget: Boolean = false,
    val budgetSaveFailed: Boolean = false,
    val budgetSaveVersion: Int = 0,
) {
    val isEmpty: Boolean
        get() = loadStatus == HomeLoadStatus.READY &&
                summary?.month == selectedMonth &&
                recentTransactions.isEmpty()

    val canShowData: Boolean
        get() = summary?.month == selectedMonth &&
                error != HomeError.CALCULATION_OVERFLOW
}
