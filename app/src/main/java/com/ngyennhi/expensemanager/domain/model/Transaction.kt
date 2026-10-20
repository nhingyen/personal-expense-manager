package com.ngyennhi.expensemanager.domain.model

import java.time.LocalDate

data class Transaction(
    val id: Long,
    val type: TransactionType,
    val amountVnd: Long,
    val category: String,
    val date: LocalDate,
    val note: String?,
)

data class TransactionDraft(
    val type: TransactionType,
    val amountVnd: Long,
    val category: String,
    val date: LocalDate,
    val note: String?,
)
