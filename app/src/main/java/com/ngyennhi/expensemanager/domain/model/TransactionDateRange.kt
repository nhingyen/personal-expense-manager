package com.ngyennhi.expensemanager.domain.model

import java.time.LocalDate

/** Both endpoints are included. */
data class TransactionDateRange(val start: LocalDate, val endInclusive: LocalDate) {
    init {
        require(!endInclusive.isBefore(start)) { "End date must not precede start date" }
    }

    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(endInclusive)
}
