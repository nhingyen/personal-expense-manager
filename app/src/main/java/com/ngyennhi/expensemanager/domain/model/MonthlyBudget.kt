package com.ngyennhi.expensemanager.domain.model

import java.math.BigInteger
import java.time.YearMonth

data class MonthlyBudget(
    val month: YearMonth,
    val limitVnd: Long,
    val spentVnd: Long,
) {
    init {
        require(limitVnd > 0)
        require(spentVnd >= 0)
    }

    val remainingVnd: Long get() = limitVnd - spentVnd
    val usedPercent: BigInteger get() = spentVnd.toBigInteger() * BigInteger.valueOf(100) / limitVnd.toBigInteger()
    val limitReached: Boolean get() = spentVnd >= limitVnd
}
