package com.ngyennhi.expensemanager.presentation.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.util.Locale

@Composable
internal fun TransactionListScreen(
    state: TransactionUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (TransactionFilter) -> Unit,
    onMonthChange: (Long) -> Unit,
    onResetFilters: () -> Unit,
    onAdd: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val searchLabel = stringResource(R.string.search_transactions)
    Scaffold(
        containerColor = ExpenseManagerColors.background,
        topBar = {
            TransactionTopBar(stringResource(R.string.transactions_title), action = {
                IconButton(onClick = onResetFilters) {
                    Image(painterResource(R.drawable.ic_filter), stringResource(R.string.reset_filters), Modifier.size(22.dp))
                }
            })
        },
        floatingActionButton = {
            if (state.hasRecordsInMonth || state.query.isNotBlank() || state.filter != TransactionFilter.ALL) {
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    modifier = Modifier.weight(1f),
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
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = { onMonthChange(-1) }) {
                    Image(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.previous_month), Modifier.size(20.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(formatMonth(state.selectedMonth, locale), style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = { onMonthChange(1) }) {
                    Image(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.next_month), Modifier.size(20.dp))
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
                        val filtered = state.query.isNotBlank() || state.filter != TransactionFilter.ALL
                        EmptyState(
                            if (filtered) R.string.no_matches_title else R.string.empty_transactions_title,
                            if (filtered) R.string.no_matches_body else R.string.empty_transactions_body,
                            if (filtered) null else onAdd,
                        )
                    }
                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
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
}

@Composable
private fun TransactionRow(record: Transaction, locale: Locale, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("transaction_row_${record.id}").clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(record.category, record.type)
        Column(Modifier.weight(1f)) {
            Text(record.category, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val type = stringResource(if (record.type == TransactionType.INCOME) R.string.type_income else R.string.type_expense)
            val date = formatDate(record.date, locale)
            val summary = if (record.note.isNullOrBlank()) {
                stringResource(R.string.transaction_row_summary, type, date)
            } else {
                stringResource(R.string.transaction_row_summary_with_note, type, date, record.note)
            }
            Text(summary, style = MaterialTheme.typography.bodySmall,
                color = ExpenseManagerColors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text(formatVnd(record.amountVnd, record.type, locale),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (record.type == TransactionType.INCOME) ExpenseManagerColors.income else ExpenseManagerColors.textPrimary)
    }
}

@Composable
private fun EmptyState(title: Int, body: Int, onAdd: (() -> Unit)?) {
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
    }
}
