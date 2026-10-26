package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.WalletRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WalletUseCasesTest {
    private val calculate = CalculateWalletBalancesUseCase()
    private val wallets = listOf(Wallet(1, "Cash", WalletType.CASH, 100), Wallet(2, "Bank", WalletType.BANK, 500))
    private fun record(id: Long, type: TransactionType, amount: Long, wallet: Long = 1) =
        Transaction(id, type, amount, "Category", LocalDate.of(2026, 10, 1), null, wallet)

    @Test fun balancesAreAllTimeIsolatedAndReassignmentNeverDuplicatesReports() {
        val records = listOf(record(1, TransactionType.INCOME, 200), record(2, TransactionType.EXPENSE, 50),
            record(3, TransactionType.EXPENSE, 25, 2).copy(date = LocalDate.of(2025, 1, 1)))
        assertEquals(listOf(250L, 475L), calculate(wallets, records).map { it.balanceVnd })
        val moved = records.map { if (it.id == 2L) it.copy(walletId = 2) else it }
        assertEquals(listOf(300L, 425L), calculate(wallets, moved).map { it.balanceVnd })
        assertEquals(725L, calculate.total(calculate(wallets, moved)))
        val month = YearMonth.of(2026, 10)
        val summary = CalculateMonthlySummaryUseCase()
        assertEquals(summary(records, month), summary(moved, month))
        assertEquals(200L, summary(moved, month).incomeVnd)
        assertEquals(50L, summary(moved, month).expenseVnd)
        assertEquals(CalculateMonthlyStatisticsUseCase(summary)(records, month), CalculateMonthlyStatisticsUseCase(summary)(moved, month))
        assertEquals(50L, CalculateMonthlyBudgetUseCase()(summary(moved, month), 100)!!.spentVnd)
        assertEquals(300L, calculate(wallets, moved.filter { it.id != 2L }).first().balanceVnd)
    }
    @Test fun exactFinalResultAvoidsIntermediateOverflowAndAllowsNegativeBalance() {
        val huge = listOf(record(1, TransactionType.INCOME, Long.MAX_VALUE), record(2, TransactionType.EXPENSE, Long.MAX_VALUE))
        assertEquals(100L, calculate(wallets.take(1), huge).single().balanceVnd)
        assertEquals(-100L, calculate(wallets.take(1), listOf(record(1, TransactionType.EXPENSE, 200))).single().balanceVnd)
        assertEquals(Long.MIN_VALUE, calculate(listOf(wallets.first().copy(openingBalanceVnd = Long.MIN_VALUE)), emptyList()).single().balanceVnd)
    }
    @Test(expected = ArithmeticException::class) fun overflowingWalletDoesNotWrap() {
        calculate(wallets, listOf(record(1, TransactionType.INCOME, Long.MAX_VALUE)))
    }
    @Test(expected = ArithmeticException::class) fun overflowingTotalDoesNotWrap() {
        calculate.total(calculate(wallets.map { it.copy(openingBalanceVnd = Long.MAX_VALUE) }, emptyList()))
    }
    @Test fun saveValidatesTrimsAndDeleteProtectsDefault() = runTest {
        val repository = object : WalletRepository {
            val rows = MutableStateFlow(emptyList<Wallet>())
            var deleteCalls = 0
            override fun observeWallets() = rows
            override suspend fun add(wallet: WalletDraft): Long { rows.value = listOf(Wallet(2, wallet.name, wallet.type, wallet.openingBalanceVnd)); return 2 }
            override suspend fun update(wallet: Wallet): Boolean { rows.value = listOf(wallet); return true }
            override suspend fun deleteEmpty(id: Long): Boolean { deleteCalls++; return true }
        }
        val save = SaveWalletUseCase(repository)
        for (name in listOf(" ", "a".repeat(61))) {
            try { save(null, WalletDraft(name, WalletType.CASH, 0)); fail("Expected validation") }
            catch (_: IllegalArgumentException) { }
        }
        save(null, WalletDraft("  Online  ", WalletType.E_WALLET, -10))
        assertEquals("Online", repository.rows.value.single().name)
        save(2, WalletDraft("Bank", WalletType.BANK, Long.MAX_VALUE))
        assertEquals(WalletType.BANK, repository.rows.value.single().type)
        assertFalse(DeleteWalletUseCase(repository)(1))
        assertEquals(0, repository.deleteCalls)
        assertTrue(DeleteWalletUseCase(repository)(2))
    }
}
