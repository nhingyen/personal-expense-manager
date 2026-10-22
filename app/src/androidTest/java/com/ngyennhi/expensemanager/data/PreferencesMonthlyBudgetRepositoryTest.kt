package com.ngyennhi.expensemanager.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.data.repository.PreferencesMonthlyBudgetRepository
import java.io.File
import java.time.YearMonth
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesMonthlyBudgetRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val month = YearMonth.of(2026, 10)

    @Test fun observesIndependentMonthsWritesDurableValuesAndClearsLimit(): Unit = runBlocking {
        val name = "budget-test-${System.nanoTime()}"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val repository = PreferencesMonthlyBudgetRepository(preferences)
        val updates = Channel<Map<YearMonth, Long>>(Channel.UNLIMITED)
        val observer = launch { repository.observeLimits().collect { updates.send(it) } }
        suspend fun await(expected: Map<YearMonth, Long>) = withTimeout(5_000) {
            var actual = updates.receive()
            while (actual != expected) actual = updates.receive()
            actual
        }
        try {
            await(emptyMap())
            repository.setLimit(month, Long.MAX_VALUE)
            await(mapOf(month to Long.MAX_VALUE))
            repository.setLimit(month.plusMonths(1), 12_000_000)
            await(mapOf(month to Long.MAX_VALUE, month.plusMonths(1) to 12_000_000L))
            val file = File(context.applicationInfo.dataDir, "shared_prefs/$name.xml")
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val entries = document.getElementsByTagName("long")
            assertEquals(2, entries.length)
            assertTrue(file.readText().contains(Long.MAX_VALUE.toString()))
            assertEquals(mapOf(month to Long.MAX_VALUE, month.plusMonths(1) to 12_000_000L),
                PreferencesMonthlyBudgetRepository(context.getSharedPreferences(name, Context.MODE_PRIVATE)).observeLimits().first())
            repository.setLimit(month, null)
            await(mapOf(month.plusMonths(1) to 12_000_000L))
        } finally {
            observer.cancel(); updates.close(); context.deleteSharedPreferences(name)
        }
    }

    @Test fun corruptStoredLimitIsAnErrorRatherThanUnsetSuccess() = runBlocking {
        val name = "budget-corrupt-${System.nanoTime()}"
        val preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        try {
            assertTrue(preferences.edit().putLong("monthly_budget:$month", 0).commit())
            try {
                PreferencesMonthlyBudgetRepository(preferences).observeLimits().first()
                fail("Corrupt budget was presented as success")
            } catch (_: IllegalArgumentException) { }
        } finally { context.deleteSharedPreferences(name) }
    }
}
