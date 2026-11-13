package com.ngyennhi.expensemanager.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.local.TransactionEntity
import com.ngyennhi.expensemanager.data.repository.RoomCategoryRepository
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.CategoryDeleteResult
import com.ngyennhi.expensemanager.domain.repository.CategorySaveResult
import com.ngyennhi.expensemanager.domain.usecase.CreateCategoryUseCase
import com.ngyennhi.expensemanager.domain.usecase.RenameCategoryUseCase
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCategoryRepositoryTest {
    private lateinit var database: ExpenseManagerDatabase
    private lateinit var repository: RoomCategoryRepository

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java)
            .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()
        repository = RoomCategoryRepository(database.categoryDao())
    }

    @After fun tearDown() = database.close()

    @Test fun createIsTrimmedCaseInsensitivePerTypeAndReactive() = runBlocking {
        val create = CreateCategoryUseCase(repository)
        assertEquals(CategorySaveResult.Saved(1), create(TransactionType.EXPENSE, " Food "))
        assertEquals(CategorySaveResult.Duplicate, create(TransactionType.EXPENSE, "fOoD"))
        assertEquals(CategorySaveResult.Saved(2), create(TransactionType.INCOME, "FOOD"))
        val rows = repository.observeCategories().first()
        assertEquals(listOf(TransactionType.EXPENSE, TransactionType.INCOME), rows.map { it.type })
        assertEquals(listOf("Food", "FOOD"), rows.map { it.name })
    }

    @Test fun renameUpdatesAllNormalizedLegacyVariantsAndDeleteProtectsHistory() = runBlocking {
        val id = (CreateCategoryUseCase(repository)(TransactionType.EXPENSE, "Food") as CategorySaveResult.Saved).id
        insertTransaction("Food")
        insertTransaction(" fOoD ")

        assertEquals(CategorySaveResult.Saved(id), RenameCategoryUseCase(repository)(id, " Dining "))
        assertEquals(listOf("Dining", "Dining"), database.transactionDao().observeAll().first().map { it.category })
        assertEquals(CategoryDeleteResult.IN_USE, repository.delete(id))
        assertTrue(repository.setArchived(id, true))
        assertTrue(repository.observeCategories().first().single().isArchived)
        assertTrue(repository.setArchived(id, false))
        assertFalse(repository.observeCategories().first().single().isArchived)
        assertEquals(listOf("Dining", "Dining"), database.transactionDao().observeAll().first().map { it.category })
    }

    @Test fun duplicateRenameDoesNotPartiallyChangeTransactions() = runBlocking {
        val create = CreateCategoryUseCase(repository)
        val food = (create(TransactionType.EXPENSE, "Food") as CategorySaveResult.Saved).id
        create(TransactionType.EXPENSE, "Dining")
        insertTransaction("Food")

        assertEquals(CategorySaveResult.Duplicate, RenameCategoryUseCase(repository)(food, " dining "))
        assertEquals("Food", database.transactionDao().observeAll().first().single().category)
        assertEquals(listOf("Dining", "Food"), repository.observeCategories().first().map { it.name })
    }

    @Test fun unusedCategoryCanBeDeletedAndMissingOperationsAreExplicit() = runBlocking {
        val id = (CreateCategoryUseCase(repository)(TransactionType.INCOME, "Gift") as CategorySaveResult.Saved).id
        assertEquals(CategoryDeleteResult.DELETED, repository.delete(id))
        assertEquals(CategoryDeleteResult.NOT_FOUND, repository.delete(id))
        assertFalse(repository.setArchived(id, true))
        assertTrue(repository.observeCategories().first().isEmpty())
    }

    private suspend fun insertTransaction(category: String) {
        database.transactionDao().insert(TransactionEntity(
            type = TransactionType.EXPENSE.name,
            amountVnd = 1,
            category = category,
            dateEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            note = null,
        ))
    }
}
