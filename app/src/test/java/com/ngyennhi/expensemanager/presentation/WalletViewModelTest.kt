package com.ngyennhi.expensemanager.presentation

import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.*
import com.ngyennhi.expensemanager.domain.usecase.*
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.wallet.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {
    private val wallets = FakeWallets()
    private val records = FakeTransactions()
    private fun viewModel() = WalletViewModel(ObserveWalletsUseCase(wallets), ObserveTransactionsUseCase(records),
        SaveWalletUseCase(wallets), DeleteWalletUseCase(wallets), CalculateWalletBalancesUseCase())
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun observationRecalculatesAfterMoveEditDeleteAndOpeningChange() = runTest {
        val vm = viewModel(); runCurrent()
        assertEquals(LoadStatus.READY, vm.uiState.value.status)
        records.rows.value = listOf(Transaction(1, TransactionType.EXPENSE, 150, "Food", LocalDate.now(), null))
        runCurrent(); assertEquals(-50L, vm.uiState.value.balances.first().balanceVnd)
        records.rows.value = records.rows.value.map { it.copy(walletId = 2) }
        runCurrent(); assertEquals(listOf(100L, 50L), vm.uiState.value.balances.map { it.balanceVnd })
        records.rows.value = records.rows.value.map { it.copy(type = TransactionType.INCOME) }
        runCurrent(); assertEquals(350L, vm.uiState.value.balances.last().balanceVnd)
        records.rows.value = emptyList(); runCurrent()
        assertEquals(300L, vm.uiState.value.totalVnd)
        wallets.rows.value = wallets.rows.value.map { if (it.id == 2L) it.copy(openingBalanceVnd = -20) else it }
        runCurrent(); assertEquals(80L, vm.uiState.value.totalVnd)
    }
    @Test fun readErrorHidesDataAndRetryRecoversWithoutDiscardingDraft() = runTest {
        wallets.readFails = true
        val vm = viewModel(); runCurrent()
        assertEquals(LoadStatus.ERROR, vm.uiState.value.status)
        assertNull(vm.uiState.value.totalVnd)
        vm.setName("Draft")
        wallets.readFails = false; vm.retry(); runCurrent()
        assertEquals(LoadStatus.READY, vm.uiState.value.status)
        assertEquals("Draft", vm.uiState.value.form.name)
        assertEquals(300L, vm.uiState.value.totalVnd)
    }
    @Test fun overflowHidesTotalsAndRecoversOnNextEmission() = runTest {
        val vm = viewModel(); runCurrent()
        wallets.rows.value = wallets.rows.value.map { it.copy(openingBalanceVnd = Long.MAX_VALUE) }
        runCurrent(); assertEquals(WalletError.OVERFLOW, vm.uiState.value.error)
        assertNull(vm.uiState.value.totalVnd); assertTrue(vm.uiState.value.balances.isEmpty())
        wallets.rows.value = wallets.rows.value.take(1)
        runCurrent(); assertNull(vm.uiState.value.error); assertEquals(Long.MAX_VALUE, vm.uiState.value.totalVnd)
    }
    @Test fun validationWriteFailureAndRetryRetainUserInput() = runTest {
        val vm = viewModel(); runCurrent(); vm.startAdd(); vm.save()
        assertTrue(vm.uiState.value.form.invalidName)
        vm.setName(" New ")
        for (invalid in listOf("", "1.2", "1,000", "9223372036854775808", "-9223372036854775809")) {
            vm.setOpening(invalid); vm.save(); assertTrue(vm.uiState.value.form.invalidOpening)
        }
        vm.setOpening("-20"); wallets.writeFails = true; vm.save(); runCurrent()
        assertEquals(WalletError.WRITE, vm.uiState.value.error)
        assertEquals("-20", vm.uiState.value.form.openingInput)
        assertFalse(vm.uiState.value.writing)
        wallets.writeFails = false; vm.save(); runCurrent()
        assertEquals("New", wallets.rows.value.last().name)
    }
    @Test fun duplicateWritesAndDraftChangesAreBlockedUntilWriteCompletes() = runTest {
        val vm = viewModel(); runCurrent(); vm.startAdd(); vm.setName("Stable")
        wallets.gate = CompletableDeferred(); vm.save(); runCurrent()
        vm.save(); vm.setName("Changed"); vm.startAdd(); vm.delete(2)
        assertTrue(vm.uiState.value.writing)
        assertEquals("Stable", vm.uiState.value.form.name)
        wallets.gate!!.complete(Unit); runCurrent()
        assertEquals(1, wallets.addCalls)
        assertFalse(vm.uiState.value.writing)
    }
    @Test fun blockedDeleteAndWriteFailureNeverEmitSuccess() = runTest {
        val vm = viewModel(); runCurrent()
        vm.delete(1); runCurrent(); assertEquals(WalletError.DELETE_BLOCKED, vm.uiState.value.error)
        wallets.allowDelete = false; vm.delete(2); runCurrent()
        assertEquals(WalletError.DELETE_BLOCKED, vm.uiState.value.error)
        wallets.writeFails = true; vm.delete(2); runCurrent()
        assertEquals(WalletError.WRITE, vm.uiState.value.error)
        assertEquals(2, wallets.rows.value.size)
    }
    private class FakeWallets : WalletRepository {
        val rows = MutableStateFlow(listOf(Wallet(1, "", WalletType.CASH, 100), Wallet(2, "Bank", WalletType.BANK, 200)))
        var readFails = false
        var writeFails = false
        var allowDelete = true
        var gate: CompletableDeferred<Unit>? = null
        var addCalls = 0
        override fun observeWallets() = flow { if (readFails) error("Read"); emitAll(rows) }
        override suspend fun add(wallet: WalletDraft): Long {
            gate?.await(); if (writeFails) error("Write"); addCalls++
            rows.value += Wallet(3, wallet.name, wallet.type, wallet.openingBalanceVnd); return 3
        }
        override suspend fun update(wallet: Wallet): Boolean { if (writeFails) error("Write"); rows.value = rows.value.map { if (it.id == wallet.id) wallet else it }; return true }
        override suspend fun deleteEmpty(id: Long): Boolean { if (writeFails) error("Write"); if (!allowDelete) return false; rows.value = rows.value.filter { it.id != id }; return true }
    }
    private class FakeTransactions : TransactionRepository {
        val rows = MutableStateFlow(emptyList<Transaction>())
        override fun observeTransactions() = rows
        override suspend fun add(transaction: TransactionDraft) = 1L
        override suspend fun update(transaction: Transaction) = true
        override suspend fun delete(id: Long) = true
    }
}
