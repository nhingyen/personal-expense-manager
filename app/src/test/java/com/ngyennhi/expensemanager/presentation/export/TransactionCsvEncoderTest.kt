package com.ngyennhi.expensemanager.presentation.export

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.model.Wallet
import com.ngyennhi.expensemanager.domain.model.WalletType
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.wallet.WalletUiState
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCsvEncoderTest {
    @Test fun emptySnapshotProducesUtf8BomHeaderAndCrlf() {
        val document = TransactionCsvEncoder.encode(snapshot())

        assertEquals(0, document.transactionCount)
        assertEquals(
            "\uFEFFid,type,amount_vnd,category,date,note,wallet_id,wallet_name,wallet_type\r\n",
            document.bytes.toString(StandardCharsets.UTF_8),
        )
        assertTrue(document.bytes.take(3).toByteArray().contentEquals(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())))
    }

    @Test fun recordsUseStableRawValuesAndPreserveInputOrder() {
        val recent = transaction(8, TransactionType.EXPENSE, Long.MAX_VALUE, "Ăn uống", "Bữa trưa 🙂", 3)
        val older = transaction(2, TransactionType.INCOME, 1, "Salary", null, 1,
            LocalDate.of(2024, 2, 29))

        val csv = TransactionCsvEncoder.encode(snapshot(
            transactions = listOf(recent, older),
            wallets = listOf(
                Wallet(3, "Ví điện tử", WalletType.E_WALLET, 0),
                Wallet(1, "", WalletType.CASH, 0),
            ),
        )).bytes.toString(StandardCharsets.UTF_8)

        assertEquals(
            "\uFEFFid,type,amount_vnd,category,date,note,wallet_id,wallet_name,wallet_type\r\n" +
                "8,EXPENSE,9223372036854775807,Ăn uống,2026-11-12,Bữa trưa 🙂,3,Ví điện tử,E_WALLET\r\n" +
                "2,INCOME,1,Salary,2024-02-29,,1,,CASH\r\n",
            csv,
        )
    }

    @Test fun commasQuotesAndNewlinesAreRfc4180Escaped() {
        val csv = TransactionCsvEncoder.encode(snapshot(
            transactions = listOf(transaction(
            id = 1,
            type = TransactionType.EXPENSE,
            amount = 50,
            category = "Food, drink",
            note = "He said \"hello\"\r\nSecond line",
            walletId = 2,
        )), wallets = listOf(Wallet(2, "Cash, daily", WalletType.CASH, 0)),
        )).bytes.toString(StandardCharsets.UTF_8)

        assertTrue(csv.contains("\"Food, drink\""))
        assertTrue(csv.contains("\"He said \"\"hello\"\"\r\nSecond line\""))
        assertTrue(csv.contains("\"Cash, daily\""))
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test fun userTextThatCouldBecomeAFormulaIsPrefixedWithoutChangingSafeText() {
        val csv = TransactionCsvEncoder.encode(snapshot(
            transactions = listOf(transaction(
            id = 1,
            type = TransactionType.EXPENSE,
            amount = 1,
            category = "  =SUM(A1:A2)",
            note = "+cmd",
            walletId = 2,
        )), wallets = listOf(Wallet(2, "@wallet", WalletType.BANK, 0)),
        )).bytes.toString(StandardCharsets.UTF_8)

        assertTrue(csv.contains("'  =SUM(A1:A2)"))
        assertTrue(csv.contains("'+cmd"))
        assertTrue(csv.contains("'@wallet"))
    }

    @Test fun nonReadySnapshotAndMissingWalletReferencesAreRejected() {
        assertFails { TransactionCsvEncoder.encode(WalletUiState()) }
        assertFails {
            TransactionCsvEncoder.encode(snapshot(
                transactions = listOf(transaction(1, TransactionType.EXPENSE, 1, "Food", null, 99)),
            ))
        }
    }

    private fun transaction(
        id: Long,
        type: TransactionType,
        amount: Long,
        category: String,
        note: String?,
        walletId: Long,
        date: LocalDate = LocalDate.of(2026, 11, 12),
    ) = Transaction(id, type, amount, category, date, note, walletId)

    private fun snapshot(
        transactions: List<Transaction> = emptyList(),
        wallets: List<Wallet> = emptyList(),
    ) = WalletUiState(status = LoadStatus.READY, transactions = transactions, wallets = wallets)

    private fun assertFails(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected export to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}
