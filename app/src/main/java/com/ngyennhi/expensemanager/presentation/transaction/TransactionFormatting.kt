package com.ngyennhi.expensemanager.presentation.transaction

import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatVnd(amount: Long, type: TransactionType, locale: Locale): String {
    val sign = if (type == TransactionType.INCOME) "+" else "−"
    return "$sign${NumberFormat.getIntegerInstance(locale).format(amount)} ₫"
}

fun formatDate(date: LocalDate, locale: Locale): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale))

fun formatMonth(month: YearMonth, locale: Locale): String =
    month.format(DateTimeFormatter.ofPattern("LLLL, yyyy", locale))
