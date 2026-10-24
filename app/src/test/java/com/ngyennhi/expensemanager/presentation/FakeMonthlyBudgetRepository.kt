package com.ngyennhi.expensemanager.presentation

import com.ngyennhi.expensemanager.domain.repository.MonthlyBudgetRepository
import java.time.YearMonth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeMonthlyBudgetRepository : MonthlyBudgetRepository {
    val limits = MutableStateFlow<Map<YearMonth, Long>>(emptyMap())
    var failObservation = false
    var failWrite = false
    var gate: CompletableDeferred<Unit>? = null
    var writeGate: CompletableDeferred<Unit>? = null
    var writes = 0
    var active = 0
    var maxActive = 0
    override fun observeLimits() = flow {
        active++
        maxActive = maxOf(active, maxActive)
        try {
            gate?.await()
            if (failObservation) error("Read failed")
            limits.collect { emit(it) }
        } finally { active-- }
    }
    override suspend fun setLimit(month: YearMonth, amountVnd: Long?) {
        writes++
        writeGate?.await()
        if (failWrite) error("Write failed")
        limits.value = if (amountVnd == null) limits.value - month else limits.value + (month to amountVnd)
    }
}
