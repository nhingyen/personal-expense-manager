package com.ngyennhi.expensemanager.presentation.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDateRange
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.usecase.AddTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.DeleteTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.FilterTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val observeTransactions: ObserveTransactionsUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val filterTransactions: FilterTransactionsUseCase = FilterTransactionsUseCase(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = mutableState.asStateFlow()

    private val navigationChannel = Channel<TransactionNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = navigationChannel.receiveAsFlow()

    private var observationJob: Job? = null

    init {
        retryObservation()
    }

    fun retryObservation() {
        observationJob?.cancel()
        update { it.copy(loadStatus = LoadStatus.LOADING) }
        observationJob = viewModelScope.launch {
            observeTransactions()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    update { it.copy(loadStatus = LoadStatus.ERROR) }
                }
                .collect { records ->
                    val current = mutableState.value
                    val editing = current.form.editingId?.let { id -> records.firstOrNull { it.id == id } }
                    update { current.copy(
                        loadStatus = LoadStatus.READY,
                        transactions = records,
                        form = if (!current.form.initialized && editing != null) editing.asForm() else current.form,
                    ) }
                }
        }
    }

    fun setQuery(value: String) = update { it.copy(query = value) }

    fun setFilter(value: TransactionFilter) = update { it.copy(filter = value) }

    fun changeMonth(delta: Long) = update {
        it.copy(selectedMonth = it.selectedMonth.plusMonths(delta), dateRange = null)
    }

    fun selectMonth(month: YearMonth) = update { it.copy(selectedMonth = month, dateRange = null) }

    fun applyFilters(category: String?, dateRange: TransactionDateRange?) = update {
        it.copy(selectedCategory = category?.trim()?.takeIf(String::isNotEmpty), dateRange = dateRange)
    }

    fun showAllForMonth(month: YearMonth) = update {
        it.copy(selectedMonth = month, query = "", filter = TransactionFilter.ALL,
            selectedCategory = null, dateRange = null)
    }

    fun resetFilters() = update {
        it.copy(query = "", filter = TransactionFilter.ALL, selectedMonth = YearMonth.now(),
            selectedCategory = null, dateRange = null)
    }

    fun selectRecord(id: Long) = update { it.copy(selectedId = id, actionError = null) }

    fun startAdd() = update {
        it.copy(form = TransactionFormState(date = LocalDate.now()), selectedId = null, actionError = null)
    }

    fun startEdit(id: Long) = update { state ->
        val record = state.transactions.firstOrNull { it.id == id }
        state.copy(
            selectedId = id,
            form = record?.asForm() ?: TransactionFormState(editingId = id, initialized = false),
            actionError = null,
        )
    }

    fun setType(value: TransactionType) = update { it.copy(form = it.form.copy(type = value), actionError = null) }

    fun setAmount(value: String) = update {
        it.copy(form = it.form.copy(amountInput = value, amountError = null), actionError = null)
    }

    fun setCategory(value: String) = update {
        it.copy(form = it.form.copy(category = value, categoryError = null), actionError = null)
    }

    fun setDate(value: LocalDate) = update { it.copy(form = it.form.copy(date = value), actionError = null) }

    fun setNote(value: String) = update { it.copy(form = it.form.copy(note = value.take(200)), actionError = null) }

    fun clearActionError() = update { it.copy(actionError = null) }

    fun save() {
        val state = mutableState.value
        if (state.form.isSaving || state.form.editingId != null && state.editRecordMissing) return

        val form = state.form
        val amount = form.amountInput.trim().takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toLongOrNull()
        val amountError = when {
            form.amountInput.isBlank() -> FieldError.REQUIRED
            amount == null || amount <= 0 -> FieldError.INVALID_AMOUNT
            else -> null
        }
        val categoryError = if (form.category.isBlank()) FieldError.REQUIRED else null
        if (amountError != null || categoryError != null) {
            update {
                it.copy(form = it.form.copy(amountError = amountError, categoryError = categoryError))
            }
            return
        }

        val validAmount = requireNotNull(amount)
        update { it.copy(form = it.form.copy(isSaving = true), actionError = null) }
        viewModelScope.launch {
            try {
                val draft = TransactionDraft(form.type, validAmount, form.category.trim(), form.date,
                    form.note.trim().ifEmpty{ null })
                val saved = if (form.editingId == null) {
                    addTransaction(draft)
                    true
                } else {
                    updateTransaction(Transaction(form.editingId, draft.type, draft.amountVnd,
                        draft.category, draft.date, draft.note))
                }
                if (saved) {
                    update { it.copy(selectedMonth = YearMonth.from(form.date)) }
                    navigationChannel.send(TransactionNavigationEvent.SAVED)
                }
                else update { it.copy(actionError = ActionError.RECORD_UNAVAILABLE) }
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Exception) {
                update { it.copy(actionError = ActionError.SAVE_FAILED) }
            } finally {
                update { it.copy(form = it.form.copy(isSaving = false)) }
            }
        }
    }

    fun deleteSelected() {
        val state = mutableState.value
        if (state.isDeleting) return
        val id = state.selectedId ?: return
        update { it.copy(isDeleting = true, actionError = null) }
        viewModelScope.launch {
            try {
                if (deleteTransaction(id)) navigationChannel.send(TransactionNavigationEvent.DELETED)
                else update { it.copy(actionError = ActionError.RECORD_UNAVAILABLE) }
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Exception) {
                update { it.copy(actionError = ActionError.DELETE_FAILED) }
            } finally {
                update { it.copy(isDeleting = false) }
            }
        }
    }

    private fun update(block: (TransactionUiState) -> TransactionUiState) {
        val state = block(mutableState.value)
        val type = when (state.filter) {
            TransactionFilter.ALL -> null
            TransactionFilter.EXPENSE -> TransactionType.EXPENSE
            TransactionFilter.INCOME -> TransactionType.INCOME
        }
        mutableState.value = state.copy(visibleTransactions = filterTransactions(
            state.transactions, state.period, state.query, type, state.selectedCategory,
        ))
    }

    private fun Transaction.asForm() = TransactionFormState(
        editingId = id,
        type = type,
        amountInput = amountVnd.toString(),
        category = category,
        date = date,
        note = note.orEmpty(),
    )
}
