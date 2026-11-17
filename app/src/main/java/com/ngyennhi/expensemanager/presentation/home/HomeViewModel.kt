
package com.ngyennhi.expensemanager.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.CategoryBudgetLimit
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetWriteResult
import com.ngyennhi.expensemanager.domain.usecase.CalculateCategoryBudgetsUseCase
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlySummaryUseCase
import com.ngyennhi.expensemanager.domain.usecase.InconsistentCategoryBudgetSnapshotException
import com.ngyennhi.expensemanager.domain.usecase.ObserveCategoriesUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveCategoryBudgetsUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveMonthlyBudgetsUseCase
import com.ngyennhi.expensemanager.domain.usecase.SaveMonthlyBudgetUseCase
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlyBudgetUseCase
import com.ngyennhi.expensemanager.domain.usecase.SaveCategoryBudgetUseCase
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val observeTransactions: ObserveTransactionsUseCase,
    private val calculateMonthlySummary: CalculateMonthlySummaryUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val observeBudgets: ObserveMonthlyBudgetsUseCase,
    private val saveBudget: SaveMonthlyBudgetUseCase,
    private val calculateBudget: CalculateMonthlyBudgetUseCase,
    private val observeCategories: ObserveCategoriesUseCase,
    private val observeCategoryBudgets: ObserveCategoryBudgetsUseCase,
    private val saveCategoryBudget: SaveCategoryBudgetUseCase,
    private val calculateCategoryBudgets: CalculateCategoryBudgetsUseCase,
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
    private var budgetInputEdited = false
    private var categoryBudgetObservationJob: Job? = null
    private var latestCatalog: List<Category>? = null
    private var categoryBudgetLimits: List<CategoryBudgetLimit>? = null

    init {
        retryObservation()
        retryBudgets()
        retryCategoryBudgets()
    }

    // User clicks the previous or next month
    fun changeMonth(delta: Long) {
        val current = mutableState.value
        if (current.isWritingAnyBudget) return
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
        // A draft belongs to one month; never carry it into a different month's editor.
        startBudgetEdit()
        dismissCategoryBudgetEditor()
    }

    fun refreshGreeting() {
        mutableState.value = mutableState.value.copy(greetingDateTime = LocalDateTime.now())
    }

    fun retryBudgets() {
        budgetObservationJob?.cancel()
        budgetLimits = null
        mutableState.value = mutableState.value.copy(budgetLoadStatus = HomeLoadStatus.LOADING,
            budget = null, budgetLimitVnd = null)
        budgetObservationJob = viewModelScope.launch {
            observeBudgets().catch { cause ->
                if (cause is CancellationException) throw cause
                budgetLimits = null
                mutableState.value = mutableState.value.copy(budgetLoadStatus = HomeLoadStatus.ERROR,
                    budget = null, budgetLimitVnd = null)
            }.collect { limits ->
                budgetLimits = limits
                mutableState.value = mutableState.value.copy(budgetLoadStatus = HomeLoadStatus.READY)
                refreshBudget()
            }
        }
    }

    fun retryCategoryBudgets() {
        categoryBudgetObservationJob?.cancel()
        latestCatalog = null
        categoryBudgetLimits = null
        val state = mutableState.value
        mutableState.value = state.copy(
            categoryBudgetLoadStatus = HomeLoadStatus.LOADING,
            categoryBudgetError = null,
            categoryBudgetStale = state.categoryBudgetItems.isNotEmpty(),
        )
        categoryBudgetObservationJob = viewModelScope.launch {
            combine(observeCategories(), observeCategoryBudgets()) { categories, limits ->
                categories to limits
            }.catch { cause ->
                if (cause is CancellationException) throw cause
                mutableState.value = mutableState.value.copy(
                    categoryBudgetLoadStatus = HomeLoadStatus.ERROR,
                    categoryBudgetError = CategoryBudgetError.LOAD_FAILED,
                    categoryBudgetStale = mutableState.value.categoryBudgetItems.isNotEmpty(),
                )
            }.collect { (categories, limits) ->
                latestCatalog = categories
                categoryBudgetLimits = limits
                refreshCategoryBudgets()
            }
        }
    }

    fun startBudgetEdit() {
        val state = mutableState.value
        if (state.isWritingAnyBudget) return
        budgetInputEdited = false
        mutableState.value = state.copy(budgetInput = state.budgetLimitVnd?.toString().orEmpty(),
            budgetInputInvalid = false, budgetSaveFailed = false)
    }

    fun setBudgetInput(value: String) {
        if (mutableState.value.isWritingAnyBudget) return
        budgetInputEdited = true
        mutableState.value = mutableState.value.copy(budgetInput = value, budgetInputInvalid = false, budgetSaveFailed = false)
    }

    fun saveMonthlyBudget() {
        if (mutableState.value.isWritingAnyBudget) return
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
        if (state.isWritingAnyBudget || state.budgetLoadStatus != HomeLoadStatus.READY) return
        val month = state.selectedMonth
        mutableState.value = state.copy(isSavingBudget = true, budgetSaveFailed = false)
        viewModelScope.launch {
            try {
                saveBudget(month, amount)
                budgetInputEdited = false
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
            budgetInput = if (!budgetInputEdited && !state.isSavingBudget) limit?.toString().orEmpty() else state.budgetInput,
            budget = if (state.canShowData && state.budgetLoadStatus == HomeLoadStatus.READY) {
                state.summary?.let { calculateBudget(it, limit) }
            } else null)
    }

    fun startAddCategoryBudget() {
        val state = mutableState.value
        if (state.isWritingAnyBudget || state.categoryBudgetLoadStatus != HomeLoadStatus.READY) return
        val configuredIds = state.categoryBudgetItems.mapTo(mutableSetOf()) { it.category.id }
        val firstAvailable = state.activeExpenseCategories.firstOrNull { it.id !in configuredIds } ?: return
        mutableState.value = state.copy(
            categoryBudgetEditor = CategoryBudgetEditorState(categoryId = firstAvailable.id),
            categoryBudgetWriteFailed = false,
        )
    }

    fun startEditCategoryBudget(categoryId: Long) {
        val state = mutableState.value
        if (state.isWritingAnyBudget || state.categoryBudgetLoadStatus != HomeLoadStatus.READY) return
        val item = state.categoryBudgetItems.firstOrNull { it.category.id == categoryId } ?: return
        if (item.category.isArchived) return
        mutableState.value = state.copy(
            categoryBudgetEditor = CategoryBudgetEditorState(
                categoryId = categoryId,
                input = item.budget.limitVnd.toString(),
                isEditing = true,
            ),
            categoryBudgetWriteFailed = false,
        )
    }

    fun selectCategoryBudgetCategory(categoryId: Long) {
        val state = mutableState.value
        val editor = state.categoryBudgetEditor ?: return
        if (state.isWritingAnyBudget || editor.isEditing) return
        val configuredIds = state.categoryBudgetItems.mapTo(mutableSetOf()) { it.category.id }
        if (state.activeExpenseCategories.none { it.id == categoryId } || categoryId in configuredIds) return
        mutableState.value = state.copy(
            categoryBudgetEditor = editor.copy(
                categoryId = categoryId,
                inputInvalid = false,
                writeFailed = false,
            ),
        )
    }

    fun setCategoryBudgetInput(value: String) {
        val state = mutableState.value
        val editor = state.categoryBudgetEditor ?: return
        if (state.isWritingAnyBudget) return
        mutableState.value = state.copy(
            categoryBudgetEditor = editor.copy(input = value, inputInvalid = false, writeFailed = false),
            categoryBudgetWriteFailed = false,
        )
    }

    fun saveCategoryBudget() {
        val state = mutableState.value
        val editor = state.categoryBudgetEditor ?: return
        if (state.isWritingAnyBudget) return
        val input = editor.input.trim()
        val amount = input.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toLongOrNull()
        if (editor.categoryId == null || amount == null || amount <= 0) {
            mutableState.value = state.copy(categoryBudgetEditor = editor.copy(inputInvalid = true))
            return
        }
        writeCategoryBudget(editor.categoryId, amount, closeEditor = true)
    }

    fun removeCategoryBudget(categoryId: Long) {
        val state = mutableState.value
        if (state.isWritingAnyBudget || state.categoryBudgetItems.none { it.category.id == categoryId }) return
        writeCategoryBudget(categoryId, null, closeEditor = true)
    }

    fun dismissCategoryBudgetEditor() {
        val state = mutableState.value
        if (state.isWritingAnyBudget) return
        mutableState.value = state.copy(categoryBudgetEditor = null, categoryBudgetWriteFailed = false)
    }

    private fun writeCategoryBudget(categoryId: Long, amountVnd: Long?, closeEditor: Boolean) {
        val state = mutableState.value
        if (state.isWritingAnyBudget || state.categoryBudgetLoadStatus != HomeLoadStatus.READY) return
        val month = state.selectedMonth
        mutableState.value = state.copy(
            isSavingCategoryBudget = true,
            categoryBudgetWritingCategoryId = categoryId,
            categoryBudgetWriteFailed = false,
            categoryBudgetEditor = state.categoryBudgetEditor?.copy(writeFailed = false),
        )
        viewModelScope.launch {
            try {
                val result = saveCategoryBudget(categoryId, month, amountVnd)
                if (result == CategoryBudgetWriteResult.SAVED || result == CategoryBudgetWriteResult.REMOVED) {
                    mutableState.value = mutableState.value.copy(
                        categoryBudgetEditor = if (closeEditor) null else mutableState.value.categoryBudgetEditor,
                        categoryBudgetSaveVersion = mutableState.value.categoryBudgetSaveVersion + 1,
                    )
                } else {
                    val current = mutableState.value
                    mutableState.value = current.copy(
                        categoryBudgetWriteFailed = true,
                        categoryBudgetEditor = current.categoryBudgetEditor?.copy(writeFailed = true),
                    )
                }
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Exception) {
                val current = mutableState.value
                mutableState.value = current.copy(
                    categoryBudgetWriteFailed = true,
                    categoryBudgetEditor = current.categoryBudgetEditor?.copy(writeFailed = true),
                )
            } finally {
                mutableState.value = mutableState.value.copy(
                    isSavingCategoryBudget = false,
                    categoryBudgetWritingCategoryId = null,
                )
            }
        }
    }

    private fun refreshCategoryBudgets() {
        val categories = latestCatalog ?: return
        val limits = categoryBudgetLimits ?: return
        val transactions = latestTransactions ?: return
        val state = mutableState.value
        val activeExpenses = categories
            .filter { it.type == TransactionType.EXPENSE && !it.isArchived }
            .sortedBy { it.name }
        try {
            val budgets = calculateCategoryBudgets(categories, transactions, limits, state.selectedMonth)
            val categoryById = categories.associateBy { it.id }
            val items = budgets.map { budget ->
                CategoryBudgetItem(
                    category = checkNotNull(categoryById[budget.categoryId]),
                    budget = budget,
                )
            }
            mutableState.value = state.copy(
                categoryBudgetLoadStatus = HomeLoadStatus.READY,
                categoryBudgetItems = items,
                activeExpenseCategories = activeExpenses,
                categoryBudgetError = null,
                categoryBudgetStale = false,
                categoryBudgetEditor = state.categoryBudgetEditor?.takeIf { editor ->
                    activeExpenses.any { it.id == editor.categoryId }
                },
            )
        } catch (_: InconsistentCategoryBudgetSnapshotException) {
            mutableState.value = state.copy(
                categoryBudgetLoadStatus = HomeLoadStatus.ERROR,
                activeExpenseCategories = activeExpenses,
                categoryBudgetError = CategoryBudgetError.INCONSISTENT_SNAPSHOT,
                categoryBudgetStale = state.categoryBudgetItems.isNotEmpty(),
            )
        } catch (_: ArithmeticException) {
            mutableState.value = state.copy(
                categoryBudgetLoadStatus = HomeLoadStatus.ERROR,
                activeExpenseCategories = activeExpenses,
                categoryBudgetError = CategoryBudgetError.CALCULATION_OVERFLOW,
                categoryBudgetStale = state.categoryBudgetItems.isNotEmpty(),
            )
        }
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
                        isStale = state.summary?.month == state.selectedMonth,
                        categoryBudgetStale = state.categoryBudgetItems.isNotEmpty(),
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
            refreshCategoryBudgets()

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
            refreshCategoryBudgets()
        }
    }
}
