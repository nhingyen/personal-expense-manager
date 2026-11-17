package com.ngyennhi.expensemanager.presentation

import com.ngyennhi.expensemanager.domain.model.CategoryBudgetLimit
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetRepository
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetWriteResult
import java.time.YearMonth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class FakeCategoryBudgetRepository : CategoryBudgetRepository {
    val limits = MutableStateFlow<List<CategoryBudgetLimit>>(emptyList())
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

    override suspend fun setLimit(
        categoryId: Long,
        month: YearMonth,
        amountVnd: Long?,
    ): CategoryBudgetWriteResult {
        writes++
        writeGate?.await()
        if (failWrite) error("Write failed")
        limits.value = limits.value.filterNot { it.categoryId == categoryId && it.month == month } +
            listOfNotNull(amountVnd?.let { CategoryBudgetLimit(categoryId, month, it) })
        return if (amountVnd == null) CategoryBudgetWriteResult.REMOVED else CategoryBudgetWriteResult.SAVED
    }
}
