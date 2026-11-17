package com.ngyennhi.expensemanager.presentation.home

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.CategoryBudget
import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.MonthlyBudget
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.YearMonth

enum class HomeLoadStatus { LOADING, READY, ERROR }

enum class HomeError { DATA_LOAD_FAILED, CALCULATION_OVERFLOW; }

enum class CategoryBudgetError { LOAD_FAILED, INCONSISTENT_SNAPSHOT, CALCULATION_OVERFLOW }

data class HomeCategory(val name: String, val type: TransactionType)

data class CategoryBudgetItem(
    val category: Category,
    val budget: CategoryBudget,
)

data class CategoryBudgetEditorState(
    val categoryId: Long? = null,
    val input: String = "",
    val isEditing: Boolean = false,
    val inputInvalid: Boolean = false,
    val writeFailed: Boolean = false,
)

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
    val categoryBudgetLoadStatus: HomeLoadStatus = HomeLoadStatus.LOADING,
    val categoryBudgetItems: List<CategoryBudgetItem> = emptyList(),
    val activeExpenseCategories: List<Category> = emptyList(),
    val categoryBudgetError: CategoryBudgetError? = null,
    val categoryBudgetStale: Boolean = false,
    val categoryBudgetEditor: CategoryBudgetEditorState? = null,
    val isSavingCategoryBudget: Boolean = false,
    val categoryBudgetWritingCategoryId: Long? = null,
    val categoryBudgetWriteFailed: Boolean = false,
    val categoryBudgetSaveVersion: Int = 0,
) {
    val isEmpty: Boolean
        get() = loadStatus == HomeLoadStatus.READY &&
                summary?.month == selectedMonth &&
                recentTransactions.isEmpty()

    val canShowData: Boolean
        get() = summary?.month == selectedMonth &&
                error != HomeError.CALCULATION_OVERFLOW

    val isWritingAnyBudget: Boolean
        get() = isSavingBudget || isSavingCategoryBudget
}
