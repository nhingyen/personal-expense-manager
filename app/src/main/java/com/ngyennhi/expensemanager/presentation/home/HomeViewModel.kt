
package com.ngyennhi.expensemanager.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlySummaryUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveMonthlyBudgetsUseCase
import com.ngyennhi.expensemanager.domain.usecase.SaveMonthlyBudgetUseCase
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlyBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeTransactions: ObserveTransactionsUseCase,
    private val calculateMonthlySummary: CalculateMonthlySummaryUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val observeBudgets: ObserveMonthlyBudgetsUseCase,
    private val saveBudget: SaveMonthlyBudgetUseCase,
    private val calculateBudget: CalculateMonthlyBudgetUseCase,
) : ViewModel() {

    companion object {
        // Keep the existing saved-state key so renaming the feature preserves month restoration.
        private const val MONTH_KEY = "dashboard_selected_month"
    }

    // Restore the saved month, or use the current month
    private val initialMonth: YearMonth =
        savedStateHandle.get<String>(MONTH_KEY)
            ?.let {
                runCatching { YearMonth.parse(it) }.getOrNull()
            }
            ?: YearMonth.now()

    // State for HomeScreen
    private val mutableState = MutableStateFlow(
        HomeUiState(selectedMonth = initialMonth)
    )

    val uiState: StateFlow<HomeUiState> =
        mutableState.asStateFlow()

    private var observationJob: Job? = null

    private var latestTransactions: List<Transaction>? = null

    private var sourceFailed = false
    private var budgetObservationJob: Job? = null
    private var budgetLimits: Map<YearMonth, Long>? = null

    init {
        retryObservation()
        retryBudgets()
    }

    // User clicks the previous or next month
    fun changeMonth(delta: Long) {
        val current = mutableState.value
        val newMonth = current.selectedMonth.plusMonths(delta)

        savedStateHandle[MONTH_KEY] = newMonth.toString()

        // Derive another month from the same snapshot without restarting Room.
        // Before the first snapshot, retain loading/error and pending retry status.
        mutableState.value = current.copy(selectedMonth = newMonth, budget = null,
            budgetLimitVnd = budgetLimits?.get(newMonth))
        refreshBudget()

        latestTransactions?.let { transactions ->
            updateHome(
                transactions = transactions,
                stale = sourceFailed
            )
        }
    }

    fun refreshGreeting() {
        mutableState.value = mutableState.value.copy(greetingDateTime = LocalDateTime.now())
    }

    fun retryBudgets() {
        budgetObservationJob?.cancel()
        mutableState.value = mutableState.value.copy(budgetLoadStatus = HomeLoadStatus.LOADING, budget = null)
        budgetObservationJob = viewModelScope.launch {
            observeBudgets().catch { cause ->
                if (cause is CancellationException) throw cause
                mutableState.value = mutableState.value.copy(budgetLoadStatus = HomeLoadStatus.ERROR, budget = null)
            }.collect { limits ->
                budgetLimits = limits
                mutableState.value = mutableState.value.copy(budgetLoadStatus = HomeLoadStatus.READY)
                refreshBudget()
            }
        }
    }

    fun startBudgetEdit() {
        val state = mutableState.value
        mutableState.value = state.copy(budgetInput = state.budgetLimitVnd?.toString().orEmpty(),
            budgetInputInvalid = false, budgetSaveFailed = false)
    }

    fun setBudgetInput(value: String) {
        mutableState.value = mutableState.value.copy(budgetInput = value, budgetInputInvalid = false, budgetSaveFailed = false)
    }

    fun saveMonthlyBudget() {
        val input = mutableState.value.budgetInput.trim()
        val amount = input.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toLongOrNull()
        if (amount == null || amount <= 0) {
            mutableState.value = mutableState.value.copy(budgetInputInvalid = true)
            return
        }
        writeBudget(amount)
    }

    fun clearMonthlyBudget() = writeBudget(null)

    private fun writeBudget(amount: Long?) {
        val state = mutableState.value
        if (state.isSavingBudget || state.budgetLoadStatus != HomeLoadStatus.READY) return
        val month = state.selectedMonth
        mutableState.value = state.copy(isSavingBudget = true, budgetSaveFailed = false)
        viewModelScope.launch {
            try {
                saveBudget(month, amount)
                mutableState.value = mutableState.value.copy(budgetSaveVersion = mutableState.value.budgetSaveVersion + 1)
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(budgetSaveFailed = true)
            } finally {
                mutableState.value = mutableState.value.copy(isSavingBudget = false)
            }
        }
    }

    private fun refreshBudget() {
        val state = mutableState.value
        val limit = budgetLimits?.get(state.selectedMonth)
        mutableState.value = state.copy(budgetLimitVnd = limit,
            budget = if (state.canShowData && state.budgetLoadStatus == HomeLoadStatus.READY) {
                state.summary?.let { calculateBudget(it, limit) }
            } else null)
    }

    // Start or retry data tracking
    fun retryObservation() {
        observationJob?.cancel()

        val current = mutableState.value

        mutableState.value = current.copy(
            isRetrying = true,
            loadStatus = if (latestTransactions == null) {
                HomeLoadStatus.LOADING
            } else {
                current.loadStatus
            }
        )

        observationJob = viewModelScope.launch {
            observeTransactions()
                .catch { cause ->
                    if (cause is CancellationException) throw cause

                    sourceFailed = true

                    val state = mutableState.value

                    mutableState.value = state.copy(
                        loadStatus = HomeLoadStatus.ERROR,
                        error = HomeError.DATA_LOAD_FAILED,
                        isRetrying = false,
                        isStale = state.summary?.month == state.selectedMonth
                    )
                }
                .collect { transactions ->
                    latestTransactions = transactions
                    sourceFailed = false
                    mutableState.value = mutableState.value.copy(isRetrying = false)

                    updateHome(
                        transactions = transactions,
                        stale = false
                    )
                }
        }
    }

// Calculate total income and expenses, and retrieve recent transactions
    private fun updateHome(
        transactions: List<Transaction>,
        stale: Boolean
    ) {
        val month = mutableState.value.selectedMonth
        mutableState.value = mutableState.value.copy(categories = transactions
            .map { HomeCategory(it.category, it.type) }.distinct().sortedBy { it.name })

        try {
            val summary = calculateMonthlySummary(
                transactions = transactions,
                month = month
            )

            val recent = transactions
                .filter { YearMonth.from(it.date) == month }
                .sortedWith(
                    compareByDescending<Transaction> { it.date }
                        .thenByDescending { it.id }
                )
                .take(5)

            val current = mutableState.value

            mutableState.value = current.copy(
                loadStatus = if (stale) {
                    HomeLoadStatus.ERROR
                } else {
                    HomeLoadStatus.READY
                },
                summary = summary,
                recentTransactions = recent,
                budget = if (current.budgetLoadStatus == HomeLoadStatus.READY) {
                    calculateBudget(summary, budgetLimits?.get(month))
                } else null,
                budgetLimitVnd = budgetLimits?.get(month),
                error = if (stale) {
                    HomeError.DATA_LOAD_FAILED
                } else {
                    null
                },
                isStale = stale
            )

        } catch (cause: ArithmeticException) {
            val current = mutableState.value

            mutableState.value = current.copy(
                loadStatus = HomeLoadStatus.ERROR,
                summary = null,
                budget = null,
                recentTransactions = emptyList(),
                error = HomeError.CALCULATION_OVERFLOW,
                isStale = false
            )
        }
    }
}
