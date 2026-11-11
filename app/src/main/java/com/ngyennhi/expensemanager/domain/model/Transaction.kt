package com.ngyennhi.expensemanager.domain.model

import java.time.LocalDate

const val MAX_TRANSACTION_NOTE_LENGTH = 200

fun String.transactionNoteLength(): Int = codePointCount(0, length)

fun String.truncateTransactionNote(): String {
    if (transactionNoteLength() <= MAX_TRANSACTION_NOTE_LENGTH) return this
    return substring(0, offsetByCodePoints(0, MAX_TRANSACTION_NOTE_LENGTH))
}

data class Transaction(
    val id: Long,
    val type: TransactionType,
    val amountVnd: Long,
    val category: String,
    val date: LocalDate,
    val note: String?,
    val walletId: Long = DEFAULT_WALLET_ID,
)

data class TransactionDraft(
    val type: TransactionType,
    val amountVnd: Long,
    val category: String,
    val date: LocalDate,
    val note: String?,
    val walletId: Long = DEFAULT_WALLET_ID,
)
