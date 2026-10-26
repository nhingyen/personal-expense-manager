package com.ngyennhi.expensemanager.data.mapper

import com.ngyennhi.expensemanager.data.local.TransactionEntity
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDate

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    amountVnd = amountVnd,
    category = category,
    date = LocalDate.ofEpochDay(dateEpochDay),
    note = note,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type.name,
    amountVnd = amountVnd,
    category = category,
    dateEpochDay = date.toEpochDay(),
    note = note,
)

fun TransactionDraft.toEntity(): TransactionEntity = TransactionEntity(
    type = type.name,
    amountVnd = amountVnd,
    category = category,
    dateEpochDay = date.toEpochDay(),
    note = note,
)
