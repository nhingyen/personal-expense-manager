package com.ngyennhi.expensemanager.data.repository

import com.ngyennhi.expensemanager.data.local.CategoryBudgetDao
import com.ngyennhi.expensemanager.data.local.CategoryBudgetEntity
import com.ngyennhi.expensemanager.domain.model.CategoryBudgetLimit
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetRepository
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetWriteResult
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomCategoryBudgetRepository @Inject constructor(
    private val dao: CategoryBudgetDao,
) : CategoryBudgetRepository {
    override fun observeLimits() = dao.observeAll().map { rows -> rows.map(CategoryBudgetEntity::toDomain) }

    override suspend fun setLimit(categoryId: Long, month: YearMonth, amountVnd: Long?) =
        dao.setLimit(
            entity = CategoryBudgetEntity(
                categoryId = categoryId,
                year = month.year,
                month = month.monthValue,
                limitVnd = amountVnd ?: REMOVAL_PLACEHOLDER,
            ),
            remove = amountVnd == null,
        ).toDomainResult()

    private companion object {
        const val REMOVAL_PLACEHOLDER = 1L
    }
}

private fun CategoryBudgetEntity.toDomain() = CategoryBudgetLimit(
    categoryId = categoryId,
    month = YearMonth.of(year, month),
    limitVnd = limitVnd,
)

private fun Int.toDomainResult() = when (this) {
    CategoryBudgetDao.SAVED -> CategoryBudgetWriteResult.SAVED
    CategoryBudgetDao.REMOVED -> CategoryBudgetWriteResult.REMOVED
    CategoryBudgetDao.CATEGORY_NOT_ACTIVE_EXPENSE -> CategoryBudgetWriteResult.CATEGORY_NOT_ACTIVE_EXPENSE
    else -> CategoryBudgetWriteResult.CATEGORY_NOT_FOUND
}
