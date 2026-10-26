package com.ngyennhi.expensemanager.presentation.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.transaction.TransactionRow
import com.ngyennhi.expensemanager.ui.theme.appearanceIconFilter
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun WalletListScreen(state: WalletUiState, onBack: () -> Unit, onAdd: () -> Unit,
    onOpen: (Long) -> Unit, onRecord: (Long) -> Unit, onRetry: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Scaffold(containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(R.string.wallets_title), onBack = onBack, action = {
            IconButton(onClick = onAdd, enabled = !state.writing && state.status == LoadStatus.READY,
                modifier = Modifier.testTag("wallet_add_top")) {
                Image(painterResource(R.drawable.ic_wallet_add), stringResource(R.string.wallet_add), Modifier.size(22.dp))
            }
        }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("wallet_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.status == LoadStatus.LOADING) item { CircularProgressIndicator() }
            item { WalletErrorBanner(state.error, onRetry) }
            if (state.status == LoadStatus.READY) {
                state.totalVnd?.let { total -> item {
                    WalletBalanceCard(stringResource(R.string.wallet_total), total, stringResource(R.string.wallet_total_hint))
                } }
                item { Text(stringResource(R.string.wallet_list), style = MaterialTheme.typography.titleMedium) }
                if (state.wallets.isEmpty()) item { Text(stringResource(R.string.wallet_empty)) }
                items(state.wallets, key = { it.id }) { wallet ->
                    Surface(color = ExpenseManagerColors.surface, shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().testTag("wallet_row_${wallet.id}").clickable { onOpen(wallet.id) }) {
                        Row(Modifier.heightIn(min = 88.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WalletIcon(wallet.type)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(walletName(wallet), style = MaterialTheme.typography.labelLarge)
                                state.balances.firstOrNull { it.wallet.id == wallet.id }?.let {
                                    Text(walletAmount(it.balanceVnd), style = MaterialTheme.typography.titleMedium)
                                }
                                Text(walletTypeName(wallet.type), style = MaterialTheme.typography.bodySmall,
                                    color = ExpenseManagerColors.textSecondary)
                            }
                            Image(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(18.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.textSecondary))
                        }
                    }
                }
                item {
                    Button(onClick = onAdd, enabled = !state.writing, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                        .testTag("wallet_add_button")) {
                        Image(painterResource(R.drawable.ic_plus), null, Modifier.size(20.dp), colorFilter = appearanceIconFilter(MaterialTheme.colorScheme.onPrimary))
                        Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.wallet_add))
                    }
                }
                item { Text(stringResource(R.string.wallet_manual_hint), style = MaterialTheme.typography.bodySmall,
                    color = ExpenseManagerColors.textSecondary) }
                item { Text(stringResource(R.string.home_recent_transactions), style = MaterialTheme.typography.titleMedium) }
                if (state.transactions.isEmpty()) item { Text(stringResource(R.string.wallet_no_transactions)) }
                items(state.transactions.take(5), key = { "recent_${it.id}" }) { record ->
                    Column {
                        TransactionRow(record, locale, { onRecord(record.id) })
                        state.wallets.firstOrNull { it.id == record.walletId }?.let {
                            Text(walletName(it), Modifier.padding(start = 56.dp), style = MaterialTheme.typography.bodySmall,
                                color = ExpenseManagerColors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WalletDetailScreen(state: WalletUiState, id: Long, onBack: () -> Unit, onEdit: () -> Unit,
    onDelete: () -> Unit, onAddTransaction: () -> Unit, onRecord: (Long) -> Unit, onRetry: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val wallet = state.wallets.firstOrNull { it.id == id }
    val records = state.transactions.filter { it.walletId == id }
    var confirmDelete by rememberSaveable(id) { mutableStateOf(false) }
    Scaffold(containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(wallet?.let { walletName(it) } ?: stringResource(R.string.wallet_detail),
            onBack = { if (!state.writing) onBack() }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("wallet_detail"), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.status == LoadStatus.LOADING) item { CircularProgressIndicator() }
            item { WalletErrorBanner(state.error, onRetry) }
            if (state.status == LoadStatus.READY) {
                if (wallet == null) item { Text(stringResource(R.string.wallet_missing)) }
                else {
                    item {
                        state.balances.firstOrNull { it.wallet.id == id }?.let {
                            WalletBalanceCard(stringResource(R.string.wallet_current_balance), it.balanceVnd, walletTypeName(wallet.type))
                        }
                    }
                    item {
                        Text(stringResource(R.string.wallet_opening_value, walletAmount(wallet.openingBalanceVnd)))
                        Text(stringResource(R.string.wallet_formula), style = MaterialTheme.typography.bodySmall,
                            color = ExpenseManagerColors.textSecondary)
                    }
                    item {
                        Button(onClick = onEdit, enabled = !state.writing, modifier = Modifier.fillMaxWidth().testTag("wallet_edit_button")) {
                            Text(stringResource(R.string.wallet_edit))
                        }
                        val canDelete = id != DEFAULT_WALLET_ID && records.isEmpty()
                        OutlinedButton(onClick = { confirmDelete = true }, enabled = !state.writing && canDelete,
                            modifier = Modifier.fillMaxWidth().testTag("wallet_delete_button")) { Text(stringResource(R.string.wallet_delete)) }
                        if (!canDelete) Text(stringResource(if (id == DEFAULT_WALLET_ID) R.string.wallet_default_protected
                            else R.string.wallet_delete_blocked), style = MaterialTheme.typography.bodySmall)
                    }
                    item {
                        Button(onClick = onAddTransaction, enabled = !state.writing, modifier = Modifier.fillMaxWidth()
                            .testTag("wallet_add_transaction")) { Text(stringResource(R.string.add_transaction_title)) }
                    }
                    item { Text(stringResource(R.string.wallet_transactions), style = MaterialTheme.typography.titleMedium) }
                    if (records.isEmpty()) item { Text(stringResource(R.string.wallet_no_transactions)) }
                    items(records, key = { it.id }) { record -> TransactionRow(record, locale, { onRecord(record.id) }) }
                }
            }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.wallet_delete)) },
        text = { Text(stringResource(R.string.wallet_delete_confirm)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() },
            modifier = Modifier.testTag("wallet_confirm_delete")) { Text(stringResource(R.string.confirm_delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } })
}
