package com.ngyennhi.expensemanager.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.repository.RoomCategoryRepository
import com.ngyennhi.expensemanager.data.repository.RoomTransactionRepository
import java.util.Locale
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
class CategoryMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), ExpenseManagerDatabase::class.java)
    private val name = "category-migration-test"

    @After fun cleanup() { context.deleteDatabase(name) }

    @Test fun v2ToV3PreservesTransactionsAndBuildsNormalizedCatalog() = runBlocking {
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO wallets VALUES (1, '', 'CASH', 0)")
            execSQL("INSERT INTO transactions VALUES (7, 'EXPENSE', 250, ' Food ', 19782, 'Lunch', 1)")
            execSQL("INSERT INTO transactions VALUES (9, 'EXPENSE', 300, 'fOoD', 19783, NULL, 1)")
            execSQL("INSERT INTO transactions VALUES (10, 'INCOME', 900, 'FOOD', 19784, 'Pay', 1)")
            execSQL("INSERT INTO transactions VALUES (11, 'EXPENSE', 20, '\u0110I CH\u1ee2', 19785, NULL, 1)")
            execSQL("INSERT INTO transactions VALUES (12, 'EXPENSE', 30, '\u0111i ch\u1ee3', 19786, NULL, 1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true, ExpenseManagerDatabase.MIGRATION_2_3).close()

        val db = open()
        try {
            val transactions = RoomTransactionRepository(db.transactionDao()).observeTransactions().first()
            assertEquals(listOf(12L, 11L, 10L, 9L, 7L), transactions.map { it.id })
            assertEquals(listOf("\u0111i ch\u1ee3", "\u0110I CH\u1ee2", "FOOD", "fOoD", " Food "), transactions.map { it.category })
            assertEquals(listOf(30L, 20L, 900L, 300L, 250L), transactions.map { it.amountVnd })
            assertEquals(listOf("EXPENSE", "EXPENSE", "INCOME", "EXPENSE", "EXPENSE"), transactions.map { it.type.name })
            assertEquals(listOf(null, null, "Pay", null, "Lunch"), transactions.map { it.note })
            assertEquals(listOf(19786L, 19785L, 19784L, 19783L, 19782L), transactions.map { it.date.toEpochDay() })
            assertTrue(transactions.all { it.walletId == 1L })
            val categories = RoomCategoryRepository(db.categoryDao()).observeCategories().first()
            assertEquals(3, categories.size)
            assertEquals(1, categories.count {
                it.type.name == "EXPENSE" && it.name.lowercase(Locale.ROOT) == "\u0111i ch\u1ee3"
            })
            assertTrue(categories.none { it.isArchived })
            db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        } finally { db.close() }
    }

    @Test fun v1ToV2ToV3PreservesRowsWalletAndTransactionSequence() = runBlocking {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO transactions VALUES (7, 'EXPENSE', 250, 'Food', 19782, 'Lunch')")
            execSQL("INSERT INTO transactions VALUES (99, 'EXPENSE', 1, 'Deleted', 1, NULL)")
            execSQL("DELETE FROM transactions WHERE id = 99")
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true,
            ExpenseManagerDatabase.MIGRATION_1_2, ExpenseManagerDatabase.MIGRATION_2_3).close()

        val db = open()
        try {
            val old = db.transactionDao().observeAll().first().single()
            assertEquals(7L, old.id)
            assertEquals("Food", old.category)
            assertEquals(1L, old.walletId)
            assertEquals("Food", db.categoryDao().observeAll().first().single().name)
            val next = db.transactionDao().insert(old.copy(id = 0, category = "Next"))
            assertTrue(next > 99)
        } finally { db.close() }
    }

    @Test fun editingArchivedMigratedVariantPreservesHistoricalCategory() = runBlocking {
        helper.createDatabase(name, 2).apply {
            execSQL("INSERT INTO wallets VALUES (1, '', 'CASH', 0)")
            execSQL("INSERT INTO transactions VALUES (1, 'EXPENSE', 250, 'Food', 19782, 'Original', 1)")
            execSQL("INSERT INTO transactions VALUES (2, 'EXPENSE', 300, 'fOoD', 19783, 'Legacy', 1)")
            close()
        }
        helper.runMigrationsAndValidate(name, 3, true, ExpenseManagerDatabase.MIGRATION_2_3).close()

        val db = open()
        try {
            val transactions = RoomTransactionRepository(db.transactionDao())
            val categories = RoomCategoryRepository(db.categoryDao())
            val category = categories.observeCategories().first().single()
            assertEquals("Food", category.name)
            assertTrue(categories.setArchived(category.id, true))

            val legacy = transactions.observeTransactions().first().first { it.id == 2L }
            assertTrue(transactions.update(legacy.copy(amountVnd = 999, note = "Updated")))

            val updated = transactions.observeTransactions().first().first { it.id == 2L }
            assertEquals(legacy.copy(amountVnd = 999, note = "Updated"), updated)
            assertEquals("fOoD", updated.category)
        } finally { db.close() }
    }

    private fun open() = Room.databaseBuilder(context, ExpenseManagerDatabase::class.java, name)
        .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET)
        .addMigrations(ExpenseManagerDatabase.MIGRATION_1_2, ExpenseManagerDatabase.MIGRATION_2_3)
        .build()
}
