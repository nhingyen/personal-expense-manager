package com.ngyennhi.expensemanager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {
    @Query("SELECT * FROM category_budgets ORDER BY year, month, category_id")
    fun observeAll(): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun findCategory(categoryId: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(entity: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE category_id = :categoryId AND year = :year AND month = :month")
    suspend fun delete(categoryId: Long, year: Int, month: Int): Int

    @Transaction
    suspend fun setLimit(entity: CategoryBudgetEntity, remove: Boolean): Int {
        val category = findCategory(entity.categoryId) ?: return CATEGORY_NOT_FOUND
        if (remove) {
            delete(entity.categoryId, entity.year, entity.month)
            return REMOVED
        }
        if (category.type != EXPENSE_TYPE || category.isArchived) return CATEGORY_NOT_ACTIVE_EXPENSE
        require(entity.limitVnd > 0) { "Category budget limit must be positive" }
        replace(entity)
        return SAVED
    }

    companion object {
        const val CATEGORY_NOT_FOUND = 0
        const val SAVED = 1
        const val REMOVED = 2
        const val CATEGORY_NOT_ACTIVE_EXPENSE = 3
        private const val EXPENSE_TYPE = "EXPENSE"
    }
}
