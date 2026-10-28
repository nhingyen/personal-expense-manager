package com.ngyennhi.expensemanager.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.repository.RoomTransactionRepository
import com.ngyennhi.expensemanager.data.repository.RoomWalletRepository
import com.ngyennhi.expensemanager.data.repository.PreferencesMonthlyBudgetRepository
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.usecase.*
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), ExpenseManagerDatabase::class.java)
    private val name = "wallet-migration-test"
    @After fun cleanup() { context.deleteDatabase(name) }

    @Test fun populatedV1PreservesFieldsTotalsAndSequenceAndEnforcesForeignKeys() = runBlocking {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO transactions VALUES (7, 'INCOME', 900, 'Salary', 20727, NULL)")
            execSQL("INSERT INTO transactions VALUES (9, 'EXPENSE', 250, 'Food', 20727, 'Lunch')")
            execSQL("INSERT INTO transactions VALUES (99, 'EXPENSE', 1, 'Deleted', 20727, NULL)")
            execSQL("DELETE FROM transactions WHERE id=99")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, ExpenseManagerDatabase.MIGRATION_1_2).close()
        val db = open()
        try {
            val transactions = RoomTransactionRepository(db.transactionDao())
            val wallets = RoomWalletRepository(db.walletDao())
            val old = transactions.observeTransactions().first()
            assertEquals(listOf(9L, 7L), old.map { it.id })
            assertEquals(listOf(250L, 900L), old.map { it.amountVnd })
            assertEquals(listOf("Food", "Salary"), old.map { it.category })
            assertEquals(listOf("Lunch", null), old.map { it.note })
            assertTrue(old.all { it.date.toEpochDay() == 20727L && it.walletId == 1L })
            assertEquals(listOf(TransactionType.EXPENSE, TransactionType.INCOME), old.map { it.type })
            assertEquals(Wallet(1, "", WalletType.CASH, 0), wallets.observeWallets().first().single())
            assertEquals(650L, CalculateWalletBalancesUseCase()(wallets.observeWallets().first(), old).single().balanceVnd)
            val summary = CalculateMonthlySummaryUseCase()(old, YearMonth.from(old.first().date))
            assertEquals(900L, summary.incomeVnd); assertEquals(250L, summary.expenseVnd)
            val next = transactions.add(TransactionDraft(TransactionType.EXPENSE, 1, "Next", LocalDate.now(), null))
            assertTrue("Deleted IDs must not be reused", next > 99L)
            try {
                transactions.add(TransactionDraft(TransactionType.INCOME, 1, "Missing wallet", LocalDate.now(), null, 999))
                fail("Expected FK rejection")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertFalse(wallets.deleteEmpty(1))
        } finally { db.close() }
    }
    @Test fun emptyV1MigratesAndKeepsDeletedAutoincrementSequence() = runBlocking {
        helper.createDatabase(name, 1).apply {
            execSQL("INSERT INTO transactions VALUES (99, 'EXPENSE', 1, 'Deleted', 1, NULL)")
            execSQL("DELETE FROM transactions")
            close()
        }
        helper.runMigrationsAndValidate(name, 2, true, ExpenseManagerDatabase.MIGRATION_1_2).close()
        val db = open()
        try {
            val repo = RoomTransactionRepository(db.transactionDao())
            assertTrue(repo.observeTransactions().first().isEmpty())
            assertEquals(1, db.walletDao().observeAll().first().size)
            assertTrue(repo.add(TransactionDraft(TransactionType.INCOME, 1, "New", LocalDate.now(), null)) > 99)
        } finally { db.close() }
    }
    @Test fun freshInstallReopenMoveAndGuardedDeletePreserveRecords() = runBlocking {
        var db = open()
        val bank: Long
        val txId: Long
        try {
            val wallets = RoomWalletRepository(db.walletDao())
            bank = wallets.add(WalletDraft("Bank", WalletType.BANK, -50))
            txId = RoomTransactionRepository(db.transactionDao()).add(TransactionDraft(TransactionType.INCOME, 100, "Salary", LocalDate.now(), null, bank))
            assertFalse(wallets.deleteEmpty(bank))
            try { db.openHelper.writableDatabase.execSQL("DELETE FROM wallets WHERE id = $bank"); fail("Expected FK rejection") }
            catch (_: android.database.sqlite.SQLiteConstraintException) { }
        } finally { db.close() }
        db = open()
        try {
            val wallets = RoomWalletRepository(db.walletDao())
            val repo = RoomTransactionRepository(db.transactionDao())
            val record = repo.observeTransactions().first().single()
            assertEquals(txId, record.id); assertEquals(bank, record.walletId)
            assertEquals(-50L, wallets.observeWallets().first().last().openingBalanceVnd)
            assertTrue(repo.update(record.copy(walletId = 1)))
            assertTrue(wallets.deleteEmpty(bank))
            assertEquals(1L, repo.observeTransactions().first().single().walletId)
            assertEquals(1, repo.observeTransactions().first().size)
            assertFalse(wallets.deleteEmpty(1))
        } finally { db.close() }
    }
    @Test fun migrationAndV2ReopenKeepBudgetPreferencesAndLegacyTransaction() = runBlocking {
        val preferencesName = "qa-upgrade-budgets-${System.nanoTime()}"
        val preferences = context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
        val limits = mapOf(YearMonth.of(2024, 2) to Long.MAX_VALUE,
            YearMonth.of(2026, 12) to 100L, YearMonth.of(2027, 1) to 300L)
        try {
            val budgets = PreferencesMonthlyBudgetRepository(preferences)
            limits.forEach { (month, limit) -> budgets.setLimit(month, limit) }
            assertTrue(preferences.edit().putString("unrelated_key", "retain").commit())
            val before = preferences.all.toMap()
            helper.createDatabase(name, 1).apply {
                execSQL("INSERT INTO transactions VALUES (7, 'EXPENSE', 250, 'Food', 19782, 'Leap day')")
                close()
            }
            helper.runMigrationsAndValidate(name, 2, true, ExpenseManagerDatabase.MIGRATION_1_2).close()
            val expected = Transaction(7, TransactionType.EXPENSE, 250, "Food",
                LocalDate.of(2024, 2, 29), "Leap day", DEFAULT_WALLET_ID)
            repeat(2) {
                val db = open()
                try {
                    assertEquals(listOf(expected), RoomTransactionRepository(db.transactionDao()).observeTransactions().first())
                    assertEquals(2, db.openHelper.readableDatabase.version)
                    db.openHelper.readableDatabase.query("PRAGMA foreign_key_check").use { cursor ->
                        assertFalse(cursor.moveToFirst())
                    }
                } finally { db.close() }
                val reopened = context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
                assertEquals(before, reopened.all)
                assertEquals(limits, PreferencesMonthlyBudgetRepository(reopened).observeLimits().first())
            }
        } finally { context.deleteSharedPreferences(preferencesName) }
    }
    private fun open() = Room.databaseBuilder(context, ExpenseManagerDatabase::class.java, name)
        .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET)
        .addMigrations(ExpenseManagerDatabase.MIGRATION_1_2).build()
}
