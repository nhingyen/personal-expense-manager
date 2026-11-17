package com.ngyennhi.expensemanager.domain.repository

import com.ngyennhi.expensemanager.domain.model.CategoryBudgetLimit
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

enum class CategoryBudgetWriteResult {
    SAVED,
    REMOVED,
    CATEGORY_NOT_FOUND,
    CATEGORY_NOT_ACTIVE_EXPENSE,
}

interface CategoryBudgetRepository {
    fun observeLimits(): Flow<List<CategoryBudgetLimit>>

    suspend fun setLimit(
        categoryId: Long,
        month: YearMonth,
        amountVnd: Long?,
    ): CategoryBudgetWriteResult
}
