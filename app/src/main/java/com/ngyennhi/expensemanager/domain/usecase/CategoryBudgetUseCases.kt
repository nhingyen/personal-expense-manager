package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.CategoryBudget
import com.ngyennhi.expensemanager.domain.model.CategoryBudgetLimit
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.model.normalizedCategoryName
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetRepository
import java.time.YearMonth

class InconsistentCategoryBudgetSnapshotException(message: String) : IllegalStateException(message)

class ObserveCategoryBudgetsUseCase(private val repository: CategoryBudgetRepository) {
    operator fun invoke() = repository.observeLimits()
}

class SaveCategoryBudgetUseCase(private val repository: CategoryBudgetRepository) {
    suspend operator fun invoke(categoryId: Long, month: YearMonth, amountVnd: Long?) =
        repository.setLimit(
            categoryId = categoryId.also { require(it > 0) },
            month = month,
            amountVnd = amountVnd.also { require(it == null || it > 0) },
        )
}

class CalculateCategoryBudgetsUseCase {
    operator fun invoke(
        categories: List<Category>,
        transactions: List<Transaction>,
        limits: List<CategoryBudgetLimit>,
        month: YearMonth,
    ): List<CategoryBudget> {
        val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
        val categoryById = expenseCategories.associateBy { it.id }
        val categoryByName = expenseCategories.associateByUniqueNormalizedName()
        val selectedLimits = limits.filter { it.month == month }

        selectedLimits.forEach { limit ->
            if (categoryById[limit.categoryId] == null) {
                throw InconsistentCategoryBudgetSnapshotException(
                    "Category budget ${limit.categoryId} has no matching expense category",
                )
            }
        }

        val spentByCategory = mutableMapOf<Long, Long>()
        transactions.asSequence()
            .filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }
            .forEach { transaction ->
                val category = categoryByName[transaction.category.normalizedCategoryName()]
                    ?: throw InconsistentCategoryBudgetSnapshotException(
                        "Expense transaction ${transaction.id} has no matching category",
                    )
                spentByCategory[category.id] = Math.addExact(
                    spentByCategory[category.id] ?: 0L,
                    transaction.amountVnd,
                )
            }

        return selectedLimits
            .sortedBy { it.categoryId }
            .map { limit ->
                CategoryBudget(
                    categoryId = limit.categoryId,
                    month = month,
                    limitVnd = limit.limitVnd,
                    spentVnd = spentByCategory[limit.categoryId] ?: 0L,
                )
            }
    }
}

private fun List<Category>.associateByUniqueNormalizedName(): Map<String, Category> {
    val result = mutableMapOf<String, Category>()
    forEach { category ->
        val previous = result.put(category.name.normalizedCategoryName(), category)
        if (previous != null && previous.id != category.id) {
            throw InconsistentCategoryBudgetSnapshotException(
                "Expense categories ${previous.id} and ${category.id} share a normalized name",
            )
        }
    }
    return result
}
