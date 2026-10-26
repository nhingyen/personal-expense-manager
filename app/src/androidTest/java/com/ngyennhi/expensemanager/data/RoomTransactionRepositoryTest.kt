package com.ngyennhi.expensemanager.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.repository.RoomTransactionRepository
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomTransactionRepositoryTest {
    private lateinit var database: ExpenseManagerDatabase
    private lateinit var repository: RoomTransactionRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java).addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()
        repository = RoomTransactionRepository(database.transactionDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun oneFlowCollectorSeesCrudAndStableOrdering() = runBlocking {
        val emissions = Channel<List<Transaction>>(Channel.UNLIMITED)
        val collector = launch {
            repository.observeTransactions().collect { emissions.send(it) }
        }
        try {
            assertEquals(emptyList<Transaction>(), emissions.await { it.isEmpty() })

            val older = draft(date = LocalDate.of(2026, 9, 30))
            val recent = draft(type = TransactionType.INCOME, date = LocalDate.of(2026, 10, 19))
            val olderId = repository.add(older)
            emissions.await { it.size == 1 }
            val firstId = repository.add(recent)
            emissions.await { it.size == 2 }
            val secondId = repository.add(recent.copy(category = "Bonus"))
            val ordered = emissions.await { it.size == 3 }
            assertEquals(listOf(secondId, firstId, olderId), ordered.map { it.id })

            val changed = ordered.first().copy(amountVnd = Long.MAX_VALUE, note = "Updated")
            assertTrue(repository.update(changed))
            val updated = emissions.await { it.any { record -> record.id == secondId && record.note == "Updated" } }
            assertEquals(Long.MAX_VALUE, updated.first().amountVnd)
            assertFalse(repository.update(changed.copy(id = 999L)))
            assertFalse(repository.delete(999L))

            assertTrue(repository.delete(secondId))
            val afterDelete = emissions.await { it.size == 2 && it.none { record -> record.id == secondId } }
            assertEquals(listOf(firstId, olderId), afterDelete.map { it.id })
        } finally {
            collector.cancel()
            emissions.close()
        }
    }

    @Test
    fun recordsSurviveClosingAndReopeningFileBackedDatabase() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "task002-${System.nanoTime()}.db"
        val saved = draft(type = TransactionType.INCOME, amountVnd = Long.MAX_VALUE, note = null)
        var fileDatabase = Room.databaseBuilder(context, ExpenseManagerDatabase::class.java, name).addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()
        try {
            val id = RoomTransactionRepository(fileDatabase.transactionDao()).add(saved)
            fileDatabase.close()
            fileDatabase = Room.databaseBuilder(context, ExpenseManagerDatabase::class.java, name).addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()

            assertEquals(listOf(Transaction(id, saved.type, saved.amountVnd, saved.category, saved.date, saved.note)),
                RoomTransactionRepository(fileDatabase.transactionDao()).observeTransactions().first())
        } finally {
            fileDatabase.close()
            context.deleteDatabase(name)
        }
    }

    private fun draft(
        type: TransactionType = TransactionType.EXPENSE,
        amountVnd: Long = 500,
        date: LocalDate = LocalDate.of(2026, 10, 19),
        note: String? = "Test",
    ) = TransactionDraft(type, amountVnd, "Food", date, note)

    private suspend fun Channel<List<Transaction>>.await(predicate: (List<Transaction>) -> Boolean): List<Transaction> =
        withTimeout(5_000) {
            var value = receive()
            while (!predicate(value)) value = receive()
            value
        }
}
