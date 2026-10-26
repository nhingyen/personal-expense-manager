package com.ngyennhi.expensemanager.presentation.transaction

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDateRange
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

enum class LoadStatus { LOADING, READY, ERROR }

enum class TransactionFilter { ALL, EXPENSE, INCOME }

enum class FieldError { REQUIRED, INVALID_AMOUNT }

enum class ActionError { SAVE_FAILED, DELETE_FAILED, RECORD_UNAVAILABLE }

data class TransactionFormState(
    val editingId: Long? = null,
    val initialized: Boolean = true,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountInput: String = "",
    val category: String = "",
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val amountError: FieldError? = null,
    val categoryError: FieldError? = null,
    val isSaving: Boolean = false,
)

data class TransactionUiState(
    val loadStatus: LoadStatus = LoadStatus.LOADING,
    val transactions: List<Transaction> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val query: String = "",
    val filter: TransactionFilter = TransactionFilter.ALL,
    val selectedCategory: String? = null,
    val dateRange: TransactionDateRange? = null,
    val visibleTransactions: List<Transaction> = emptyList(),
    val selectedId: Long? = null,
    val form: TransactionFormState = TransactionFormState(),
    val actionError: ActionError? = null,
    val isDeleting: Boolean = false,
) {
    val selectedRecord: Transaction?
        get() = transactions.firstOrNull { it.id == selectedId }

    val period: TransactionDateRange
        get() = dateRange ?: TransactionDateRange(selectedMonth.atDay(1), selectedMonth.atEndOfMonth())

    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || filter != TransactionFilter.ALL || selectedCategory != null || dateRange != null

    val availableCategories: List<String>
        get() = (transactions.map { it.category.trim() } + listOfNotNull(selectedCategory))
            .distinctBy { it.lowercase(Locale.ROOT) }.sortedBy { it.lowercase(Locale.ROOT) }

    val hasRecordsInMonth: Boolean
        get() = transactions.any { YearMonth.from(it.date) == selectedMonth }

    val editRecordMissing: Boolean
        get() = form.editingId != null && loadStatus == LoadStatus.READY &&
            transactions.none { it.id == form.editingId }
}

enum class TransactionNavigationEvent { SAVED, DELETED }
