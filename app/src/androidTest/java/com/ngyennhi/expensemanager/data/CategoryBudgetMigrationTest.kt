package com.ngyennhi.expensemanager.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.repository.RoomCategoryBudgetRepository
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetWriteResult
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryBudgetMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), ExpenseManagerDatabase::class.java)
    private val name = "category-budget-migration-test"

    @After fun cleanup() { context.deleteDatabase(name) }

    @Test fun v3ToV4PreservesExistingRowsAndCreatesEmptyBudgetTable() = runBlocking {
        helper.createDatabase(name, 3).apply {
            execSQL("INSERT INTO wallets VALUES (1, '', 'CASH', 0)")
            execSQL("INSERT INTO categories VALUES (5, 'EXPENSE', 'Food', 'food', 1)")
            execSQL("INSERT INTO transactions VALUES (7, 'EXPENSE', 250, 'fOoD', 19782, 'Lunch', 1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 4, true, ExpenseManagerDatabase.MIGRATION_3_4).close()

        val db = open()
        try {
            val category = db.categoryDao().observeAll().first().single()
            val transaction = db.transactionDao().observeAll().first().single()
            assertEquals(5L, category.id)
            assertEquals("Food", category.name)
            assertTrue(category.isArchived)
            assertEquals(7L, transaction.id)
            assertEquals("fOoD", transaction.category)
            assertEquals(250L, transaction.amountVnd)
            assertTrue(db.categoryBudgetDao().observeAll().first().isEmpty())
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use {
                assertFalse(it.moveToFirst())
            }
        } finally { db.close() }
    }

    @Test fun v1ToV2ToV3ToV4RetainsHistoryAndSupportsCategoryBudgets() = runBlocking {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO transactions VALUES (7, 'EXPENSE', 250, 'Food', 19782, 'Lunch')")
            execSQL("INSERT INTO transactions VALUES (99, 'EXPENSE', 1, 'Deleted', 1, NULL)")
            execSQL("DELETE FROM transactions WHERE id = 99")
            close()
        }
        helper.runMigrationsAndValidate(
            name,
            4,
            true,
            ExpenseManagerDatabase.MIGRATION_1_2,
            ExpenseManagerDatabase.MIGRATION_2_3,
            ExpenseManagerDatabase.MIGRATION_3_4,
        ).close()

        val db = open()
        try {
            val transaction = db.transactionDao().observeAll().first().single()
            val category = db.categoryDao().observeAll().first().single()
            assertEquals(7L, transaction.id)
            assertEquals("Food", transaction.category)
            assertEquals(1L, transaction.walletId)
            assertEquals("Food", category.name)

            val result = RoomCategoryBudgetRepository(db.categoryBudgetDao())
                .setLimit(category.id, YearMonth.of(2026, 11), 500)
            assertEquals(CategoryBudgetWriteResult.SAVED, result)
            assertEquals(500L, db.categoryBudgetDao().observeAll().first().single().limitVnd)

            val next = db.transactionDao().insert(transaction.copy(id = 0, category = "Next"))
            assertTrue(next > 99)
        } finally { db.close() }
    }

    private fun open() = Room.databaseBuilder(context, ExpenseManagerDatabase::class.java, name)
        .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET)
        .addMigrations(
            ExpenseManagerDatabase.MIGRATION_1_2,
            ExpenseManagerDatabase.MIGRATION_2_3,
            ExpenseManagerDatabase.MIGRATION_3_4,
        ).build()
}
