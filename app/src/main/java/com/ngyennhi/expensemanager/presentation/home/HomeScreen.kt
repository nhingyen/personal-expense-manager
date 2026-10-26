package com.ngyennhi.expensemanager.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.presentation.common.components.MonthSelector
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.presentation.transaction.TransactionRow
import com.ngyennhi.expensemanager.presentation.transaction.formatMonth
import com.ngyennhi.expensemanager.ui.theme.appearanceIconFilter
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

/** Renders ViewModel state; data observation and navigation belong to the caller. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAdd: () -> Unit,
    onViewAll: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onRetry: () -> Unit,
    homeActions: HomeActions,
    showFloatingAdd: Boolean = true,
) {
    val locale = LocalConfiguration.current.locales[0]
    var panel by rememberSaveable { mutableStateOf<HomePanel?>(null) }
    var monthPicker by rememberSaveable { mutableStateOf(false) }
    var handledSaveVersion by remember { mutableStateOf(state.budgetSaveVersion) }
    LaunchedEffect(state.budgetSaveVersion) {
        if (handledSaveVersion != state.budgetSaveVersion) {
            if (panel == HomePanel.BUDGET) panel = null
            handledSaveVersion = state.budgetSaveVersion
        }
    }
    val openBudget: () -> Unit = { homeActions.onEditBudget(); panel = HomePanel.BUDGET }
    Scaffold(
        containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(title = stringResource(R.string.home_title), action = {
            IconButton(onClick = { panel = HomePanel.NOTIFICATIONS },
                modifier = Modifier.size(48.dp).testTag("home_notifications_button")) {
                Icon(painterResource(R.drawable.ic_home_notifications), stringResource(R.string.home_notifications),
                    Modifier.size(22.dp), tint = ExpenseManagerColors.primary)
            }
        }) },
        floatingActionButton = {
            if (showFloatingAdd && !state.isEmpty) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    modifier = Modifier.testTag("home_add"),
                    containerColor = ExpenseManagerColors.primary,
                    contentColor = ExpenseManagerColors.surface,
                    icon = { Image(painterResource(R.drawable.ic_plus), null, Modifier.size(22.dp), colorFilter = appearanceIconFilter(MaterialTheme.colorScheme.onPrimary)) },
                    text = { Text(stringResource(R.string.add_transaction)) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).padding(top = 8.dp)) {
            HomeGreeting(state.greetingDateTime, state.selectedMonth, locale) { monthPicker = true }
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("home_content"),
                contentPadding = PaddingValues(bottom = if (showFloatingAdd) 96.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.error != null || state.loadStatus == HomeLoadStatus.ERROR) {
                    item {
                        ErrorBanner(
                            message = stringResource(
                                if (state.error == HomeError.CALCULATION_OVERFLOW) {
                                    R.string.home_calculation_error
                                } else {
                                    R.string.home_load_error
                                },
                            ),
                            onRetry = if (state.isRetrying) null else onRetry,
                        )
                    }
                }
                if (state.canShowData && state.isStale) {
                    item {
                        Text(
                            stringResource(R.string.home_stale_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = ExpenseManagerColors.textSecondary,
                            modifier = Modifier.testTag("home_stale").semantics {
                                liveRegion = LiveRegionMode.Polite
                            },
                        )
                    }
                }
                if (state.isRetrying || state.loadStatus == HomeLoadStatus.LOADING) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.testTag("home_loading"))
                        }
                    }
                }
                if (state.canShowData) {
                    state.summary?.let { summary -> item {
                        Box(Modifier.padding(bottom = 10.dp)) { MonthlySummaryCard(summary, locale) }
                    } }
                }
                item {
                    Column(Modifier.padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        HomeShortcuts(openBudget, { panel = HomePanel.CATEGORIES })
                        HomeBudgetCard(state, locale, openBudget, homeActions.onRetryBudgets)
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.home_recent_transactions),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).semantics { heading() },
                        )
                        TextButton(
                            onClick = onViewAll,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("home_view_all"),
                            contentPadding = PaddingValues(0.dp),
                        ) { Text(stringResource(R.string.home_view_all),
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
                    }
                }
                if (state.isEmpty) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                                .testTag("home_empty"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(stringResource(R.string.empty_transactions_title),
                                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            Text(stringResource(R.string.empty_transactions_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ExpenseManagerColors.textSecondary, textAlign = TextAlign.Center)
                            Button(onClick = onAdd, modifier = Modifier.testTag("home_add")) {
                                Text(stringResource(R.string.add_transaction))
                            }
                        }
                    }
                } else if (state.canShowData) {
                    items(state.recentTransactions, key = { it.id }) { record ->
                        TransactionRow(record, locale, onClick = { onOpenRecord(record.id) })
                    }
                }
            }
        }
    }
    if (monthPicker) {
        AlertDialog(
            onDismissRequest = { monthPicker = false },
            title = { Text(stringResource(R.string.home_choose_month)) },
            text = { MonthSelector(formatMonth(state.selectedMonth, locale), onPreviousMonth, onNextMonth) },
            confirmButton = { TextButton(onClick = { monthPicker = false }, modifier = Modifier.testTag("home_month_done")) {
                Text(stringResource(R.string.confirm))
            } },
        )
    }
    panel?.let {
        HomePanelSheet(it, state, locale, homeActions, onRetry,
            onPreviousMonth = onPreviousMonth, onNextMonth = onNextMonth, onDismiss = { panel = null })
    }
}
