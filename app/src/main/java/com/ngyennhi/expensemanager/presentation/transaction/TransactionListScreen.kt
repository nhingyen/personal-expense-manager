package com.ngyennhi.expensemanager.presentation.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.TransactionDateRange
import com.ngyennhi.expensemanager.presentation.common.components.MonthSelector
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun TransactionListScreen(
    state: TransactionUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (TransactionFilter) -> Unit,
    onApplyFilters: (String?, TransactionDateRange?) -> Unit,
    onMonthChange: (Long) -> Unit,
    onResetFilters: () -> Unit,
    onAdd: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onRetry: () -> Unit,
    showFloatingAdd: Boolean = true,
) {
    val locale = LocalConfiguration.current.locales[0]
    val searchLabel = stringResource(R.string.search_transactions)
    var showFilters by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        containerColor = ExpenseManagerColors.background,
        topBar = {
            AppTopBar(stringResource(R.string.transactions_title), action = {
                IconButton(onClick = { showFilters = true }, modifier = Modifier.testTag("open_transaction_filters")) {
                    Image(painterResource(R.drawable.ic_filter), stringResource(R.string.transaction_filters), Modifier.size(22.dp))
                }
            })
        },
        floatingActionButton = {
            if (showFloatingAdd && (state.hasRecordsInMonth || state.hasActiveFilters)) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    modifier = Modifier.testTag("add_transaction_button"),
                    containerColor = ExpenseManagerColors.primary,
                    contentColor = ExpenseManagerColors.surface,
                    icon = { Image(painterResource(R.drawable.ic_plus), contentDescription = null, modifier = Modifier.size(22.dp)) },
                    text = { Text(stringResource(R.string.add_transaction)) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().testTag("transactions_screen")
                .padding(padding).padding(horizontal = 20.dp).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp)
                    .background(ExpenseManagerColors.mutedSurface, RoundedCornerShape(100.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(painterResource(R.drawable.ic_search), contentDescription = null, modifier = Modifier.size(22.dp))
                BasicTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f).testTag("transaction_search")
                        .semantics { contentDescription = searchLabel },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = ExpenseManagerColors.textPrimary),
                    cursorBrush = SolidColor(ExpenseManagerColors.primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    decorationBox = { inner ->
                        Box {
                            if (state.query.isEmpty()) Text(searchLabel, style = MaterialTheme.typography.bodyMedium,
                                color = ExpenseManagerColors.textSecondary)
                            inner()
                        }
                    },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip(stringResource(R.string.filter_all), state.filter == TransactionFilter.ALL,
                    { onFilterChange(TransactionFilter.ALL) }, Modifier.weight(1f))
                TypeChip(stringResource(R.string.type_expense), state.filter == TransactionFilter.EXPENSE,
                    { onFilterChange(TransactionFilter.EXPENSE) }, Modifier.weight(1f))
                TypeChip(stringResource(R.string.type_income), state.filter == TransactionFilter.INCOME,
                    { onFilterChange(TransactionFilter.INCOME) }, Modifier.weight(1f))
            }
            val range = state.dateRange
            if (range == null) {
                MonthSelector(
                    label = formatMonth(state.selectedMonth, locale),
                    onPreviousMonth = { onMonthChange(-1) },
                    onNextMonth = { onMonthChange(1) },
                )
            } else {
                TextButton(onClick = { showFilters = true }, modifier = Modifier.fillMaxWidth()
                    .testTag("active_filter_range")) {
                    Text(stringResource(R.string.filter_range_value,
                        formatDate(range.start, locale), formatDate(range.endInclusive, locale)))
                }
            }
            if (state.hasActiveFilters || state.selectedMonth != java.time.YearMonth.now()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    state.selectedCategory?.let { category ->
                        TextButton(onClick = { showFilters = true }, modifier = Modifier.weight(1f)
                            .testTag("active_filter_category")) {
                            Text(stringResource(R.string.filter_category_value, category))
                        }
                    }
                    TextButton(onClick = onResetFilters, modifier = Modifier.testTag("reset_transaction_filters")) {
                        Text(stringResource(R.string.reset_filters))
                    }
                }
            }
            if (state.loadStatus == LoadStatus.ERROR) {
                ErrorBanner(stringResource(R.string.load_transactions_error), onRetry)
                if (state.transactions.isNotEmpty()) {
                    Text(stringResource(R.string.stale_transactions), style = MaterialTheme.typography.bodySmall,
                        color = ExpenseManagerColors.textSecondary)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val visible = state.visibleTransactions
                when {
                    state.loadStatus == LoadStatus.LOADING && state.transactions.isEmpty() ->
                        CircularProgressIndicator(Modifier.align(Alignment.Center).testTag("transactions_loading"))
                    state.loadStatus == LoadStatus.ERROR && state.transactions.isEmpty() -> Unit
                    visible.isEmpty() -> {
                        val filtered = state.hasActiveFilters
                        EmptyState(
                            if (filtered) R.string.no_matches_title else R.string.empty_transactions_title,
                            if (filtered) R.string.no_matches_body else R.string.empty_transactions_body,
                            if (filtered) null else onAdd,
                            if (filtered) onResetFilters else null,
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier.testTag("transaction_list"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = if (showFloatingAdd) 96.dp else 16.dp),
                    ) {
                        itemsIndexed(visible, key = { _, record -> record.id }) { index, record ->
                            if (index == 0 || visible[index - 1].date != record.date) {
                                Text(formatDate(record.date, locale), style = MaterialTheme.typography.labelMedium,
                                    color = ExpenseManagerColors.textSecondary,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                            }
                            TransactionRow(record, locale, { onOpenRecord(record.id) })
                            HorizontalDivider(color = ExpenseManagerColors.outline)
                        }
                    }
                }
            }
        }
    }
    if (showFilters) {
        TransactionFilterSheet(state, onDismiss = { showFilters = false }, onApply = { category, range ->
            onApplyFilters(category, range)
            showFilters = false
        }, onReset = {
            onResetFilters()
            showFilters = false
        })
    }
}

@Composable
private fun EmptyState(title: Int, body: Int, onAdd: (() -> Unit)?, onReset: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium,
            color = ExpenseManagerColors.textSecondary, textAlign = TextAlign.Center)
        if (onAdd != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAdd, modifier = Modifier.testTag("add_transaction_button")) {
                Text(stringResource(R.string.add_transaction))
            }
        }
        if (onReset != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onReset, modifier = Modifier.testTag("empty_reset_filters")) {
                Text(stringResource(R.string.reset_filters))
            }
        }
    }
}
