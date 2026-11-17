package com.ngyennhi.expensemanager.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.data.local.CategoryEntity
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.repository.RoomCategoryBudgetRepository
import com.ngyennhi.expensemanager.data.repository.RoomCategoryRepository
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetWriteResult
import com.ngyennhi.expensemanager.domain.repository.CategoryDeleteResult
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCategoryBudgetRepositoryTest {
    private lateinit var database: ExpenseManagerDatabase
    private lateinit var repository: RoomCategoryBudgetRepository
    private val month = YearMonth.of(2026, 11)

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java)
            .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET)
            .build()
        repository = RoomCategoryBudgetRepository(database.categoryBudgetDao())
    }

    @After fun tearDown() = database.close()

    @Test fun savesReplacesObservesAndRemovesIndependentLimits() = runBlocking {
        val food = insertCategory(TransactionType.EXPENSE, "Food")
        val travel = insertCategory(TransactionType.EXPENSE, "Travel")

        assertEquals(CategoryBudgetWriteResult.SAVED, repository.setLimit(food, month, 100))
        assertEquals(CategoryBudgetWriteResult.SAVED, repository.setLimit(food, month, 250))
        assertEquals(CategoryBudgetWriteResult.SAVED, repository.setLimit(food, month.plusMonths(1), 300))
        assertEquals(CategoryBudgetWriteResult.SAVED, repository.setLimit(travel, month, 400))
        assertEquals(
            listOf(250L, 400L, 300L),
            repository.observeLimits().first().map { it.limitVnd },
        )

        assertEquals(CategoryBudgetWriteResult.REMOVED, repository.setLimit(food, month, null))
        assertEquals(2, repository.observeLimits().first().size)
    }

    @Test fun transactionallyRejectsMissingIncomeAndArchivedCategories() = runBlocking {
        val expense = insertCategory(TransactionType.EXPENSE, "Food")
        val income = insertCategory(TransactionType.INCOME, "Salary")
        val archived = insertCategory(TransactionType.EXPENSE, "Old", archived = true)

        try {
            repository.setLimit(expense, month, 0)
            fail("Non-positive limit was persisted")
        } catch (_: IllegalArgumentException) { }
        assertEquals(CategoryBudgetWriteResult.CATEGORY_NOT_FOUND, repository.setLimit(999, month, 100))
        assertEquals(CategoryBudgetWriteResult.CATEGORY_NOT_ACTIVE_EXPENSE, repository.setLimit(income, month, 100))
        assertEquals(CategoryBudgetWriteResult.CATEGORY_NOT_ACTIVE_EXPENSE, repository.setLimit(archived, month, 100))
        assertEquals(emptyList<Any>(), repository.observeLimits().first())
    }

    @Test fun renameAndArchivePreserveLimitAndBudgetGuardsDelete() = runBlocking {
        val categoryId = insertCategory(TransactionType.EXPENSE, "Food")
        val categories = RoomCategoryRepository(database.categoryDao())
        assertEquals(CategoryBudgetWriteResult.SAVED, repository.setLimit(categoryId, month, 100))

        categories.rename(categoryId, "Dining")
        categories.setArchived(categoryId, true)
        assertEquals(categoryId, repository.observeLimits().first().single().categoryId)
        assertEquals(CategoryDeleteResult.IN_USE, categories.delete(categoryId))

        assertEquals(CategoryBudgetWriteResult.REMOVED, repository.setLimit(categoryId, month, null))
        assertEquals(CategoryDeleteResult.DELETED, categories.delete(categoryId))
    }

    private suspend fun insertCategory(type: TransactionType, name: String, archived: Boolean = false) =
        database.categoryDao().insert(CategoryEntity(
            type = type.name,
            name = name,
            normalizedName = name.lowercase(),
            isArchived = archived,
        ))
}
