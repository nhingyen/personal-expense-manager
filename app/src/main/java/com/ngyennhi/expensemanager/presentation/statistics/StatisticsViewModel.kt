package com.ngyennhi.expensemanager.presentation.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlyStatisticsUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val observeTransactions: ObserveTransactionsUseCase,
    private val calculateStatistics: CalculateMonthlyStatisticsUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val initialMonth = savedStateHandle.get<String>(MONTH_KEY)
        ?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.now()
    private val mutableState = MutableStateFlow(StatisticsUiState(selectedMonth = initialMonth))
    val uiState: StateFlow<StatisticsUiState> = mutableState.asStateFlow()
    private var observationJob: Job? = null
    private var latestTransactions: List<Transaction>? = null

    init { retryObservation() }

    fun changeMonth(delta: Long) {
        val month = mutableState.value.selectedMonth.plusMonths(delta)
        savedStateHandle[MONTH_KEY] = month.toString()
        mutableState.value = mutableState.value.copy(selectedMonth = month, statistics = null)
        latestTransactions?.let(::updateStatistics)
    }

    fun retryObservation() {
        observationJob?.cancel()
        latestTransactions = null
        mutableState.value = mutableState.value.copy(isLoading = true, statistics = null, error = null)
        observationJob = viewModelScope.launch {
            try {
                observeTransactions().collect { transactions ->
                    latestTransactions = transactions
                    updateStatistics(transactions)
                }
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Exception) {
                latestTransactions = null
                mutableState.value = mutableState.value.copy(
                    isLoading = false, statistics = null, error = StatisticsError.LOAD_FAILED)
            }
        }
    }

    private fun updateStatistics(transactions: List<Transaction>) {
        val state = mutableState.value
        mutableState.value = try {
            state.copy(isLoading = false, statistics = calculateStatistics(transactions, state.selectedMonth), error = null)
        } catch (_: ArithmeticException) {
            state.copy(isLoading = false, statistics = null, error = StatisticsError.CALCULATION_OVERFLOW)
        }
    }

    private companion object {
        const val MONTH_KEY = "statistics_selected_month"
    }
}
