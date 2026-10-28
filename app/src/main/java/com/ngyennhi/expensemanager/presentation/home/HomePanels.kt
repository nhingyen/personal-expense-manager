package com.ngyennhi.expensemanager.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.common.components.MonthSelector
import com.ngyennhi.expensemanager.presentation.transaction.CategoryIcon
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.presentation.transaction.formatMonth
import com.ngyennhi.expensemanager.presentation.transaction.suggestedCategories
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.util.Locale

data class HomeActions(
    val onEditBudget: () -> Unit,
    val onBudgetInput: (String) -> Unit,
    val onSaveBudget: () -> Unit,
    val onClearBudget: () -> Unit,
    val onRetryBudgets: () -> Unit,
    val onChooseCategory: (String, TransactionType) -> Unit,
)

internal enum class HomePanel { BUDGET, CATEGORIES, NOTIFICATIONS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomePanelSheet(
    panel: HomePanel,
    state: HomeUiState,
    locale: Locale,
    actions: HomeActions,
    onRetryTransactions: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDismiss: () -> Unit,
) {
    val blockDismiss by rememberUpdatedState(panel == HomePanel.BUDGET && state.isSavingBudget)
    ModalBottomSheet(onDismissRequest = { if (!blockDismiss) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !blockDismiss }),
        containerColor = ExpenseManagerColors.background) {
        Column(Modifier.fillMaxWidth().testTag("home_panel_content").verticalScroll(rememberScrollState()).imePadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(when (panel) {
                HomePanel.BUDGET -> R.string.home_monthly_budget
                HomePanel.CATEGORIES -> R.string.home_categories
                HomePanel.NOTIFICATIONS -> R.string.home_notifications
            }), style = MaterialTheme.typography.headlineSmall)
            if (panel == HomePanel.BUDGET) {
                MonthSelector(formatMonth(state.selectedMonth, locale), onPreviousMonth, onNextMonth,
                    enabled = !state.isSavingBudget)
            } else {
                Text(formatMonth(state.selectedMonth, locale), style = MaterialTheme.typography.labelMedium)
            }
            when (panel) {
                HomePanel.BUDGET -> {
                    if (state.error != null) {
                        ErrorBanner(stringResource(if (state.error == HomeError.CALCULATION_OVERFLOW)
                            R.string.home_calculation_error else R.string.home_load_error), onRetryTransactions)
                    }
                    if (state.isStale) Text(stringResource(R.string.home_stale_data),
                        style = MaterialTheme.typography.bodySmall)
                    MonthlyBudgetOverview(state, locale)
                    BudgetEditor(state, actions)
                    TextButton(onClick = onDismiss, enabled = !state.isSavingBudget,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.cancel))
                    }
                }
                HomePanel.CATEGORIES -> CategoryChooser(state, actions, onDismiss)
                HomePanel.NOTIFICATIONS -> BudgetNotifications(state, locale, actions.onRetryBudgets, onRetryTransactions)
            }
        }
    }
}

@Composable
private fun BudgetEditor(state: HomeUiState, actions: HomeActions) {
    when (state.budgetLoadStatus) {
        HomeLoadStatus.LOADING -> CircularProgressIndicator()
        HomeLoadStatus.ERROR -> ErrorBanner(stringResource(R.string.home_budget_load_error), actions.onRetryBudgets)
        HomeLoadStatus.READY -> {
            OutlinedTextField(
                value = state.budgetInput, onValueChange = actions.onBudgetInput,
                modifier = Modifier.fillMaxWidth().testTag("home_budget_input"), singleLine = true,
                label = { Text(stringResource(R.string.home_budget_limit)) },
                suffix = { Text(stringResource(R.string.vnd_suffix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = state.budgetInputInvalid, enabled = !state.isSavingBudget,
                supportingText = { if (state.budgetInputInvalid) Text(stringResource(R.string.amount_invalid)) },
                shape = RoundedCornerShape(14.dp),
            )
            if (state.budgetSaveFailed) ErrorBanner(stringResource(R.string.home_budget_save_error))
            Button(onClick = actions.onSaveBudget, enabled = !state.isSavingBudget,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("home_budget_save")) {
                Text(stringResource(if (state.isSavingBudget) R.string.saving_transaction else R.string.home_save_budget))
            }
            if (state.budgetLimitVnd != null) {
                TextButton(onClick = actions.onClearBudget, enabled = !state.isSavingBudget,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("home_budget_clear")) {
                    Text(stringResource(R.string.home_clear_budget), color = ExpenseManagerColors.expense)
                }
            }
        }
    }
}

@Composable
private fun CategoryChooser(state: HomeUiState, actions: HomeActions, onDismiss: () -> Unit) {
    Text(stringResource(R.string.home_choose_category_to_add), style = MaterialTheme.typography.bodyMedium)
    for (type in listOf(TransactionType.EXPENSE, TransactionType.INCOME)) {
        Text(stringResource(if (type == TransactionType.EXPENSE) R.string.type_expense else R.string.type_income),
            style = MaterialTheme.typography.titleMedium)
        val suggestions = suggestedCategories(type).map { stringResource(it) }
        val categories = (suggestions + state.categories.filter { it.type == type }.map { it.name }).distinct()
        categories.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { category ->
                    Surface(onClick = { onDismiss(); actions.onChooseCategory(category, type) },
                        modifier = Modifier.weight(1f).heightIn(min = 88.dp),
                        color = ExpenseManagerColors.surface, shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryIcon(category, type, 38.dp)
                            Text(category, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BudgetNotifications(state: HomeUiState, locale: Locale, onRetryBudgets: () -> Unit, onRetryTransactions: () -> Unit) {
    when {
        state.budgetLoadStatus == HomeLoadStatus.ERROR -> ErrorBanner(stringResource(R.string.home_budget_load_error), onRetryBudgets)
        state.error != null -> ErrorBanner(stringResource(if (state.error == HomeError.CALCULATION_OVERFLOW)
            R.string.home_calculation_error else R.string.home_load_error), onRetryTransactions)
        state.budgetLoadStatus == HomeLoadStatus.LOADING || state.loadStatus == HomeLoadStatus.LOADING -> CircularProgressIndicator()
        state.budget?.limitReached == true -> {
            val budget = state.budget
            Text(if (budget.remainingVnd < 0) stringResource(R.string.home_budget_exceeded, homeVnd(-budget.remainingVnd, locale))
                else stringResource(R.string.home_budget_reached), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("home_budget_notice"))
        }
        else -> Text(stringResource(R.string.home_no_notifications), style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("home_no_notifications"))
    }
}
