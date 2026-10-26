package com.ngyennhi.expensemanager.presentation.transaction

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun TransactionDetailScreen(
    state: TransactionUiState,
    onBack: () -> Unit,
    onReturnToList: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    walletLabel: String? = null,
    walletLoadFailed: Boolean = false,
    onRetryWallets: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val record = state.selectedRecord
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(R.string.transaction_detail_title), onBack = onBack) },
    ) { padding ->
        when {
            state.loadStatus == LoadStatus.LOADING && record == null ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.loadStatus == LoadStatus.ERROR && record == null ->
                Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                    ErrorBanner(stringResource(R.string.load_transactions_error), onRetry)
                }
            record == null -> UnavailableState(Modifier.padding(padding), onReturnToList)
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryIcon(record.category, record.type, 68.dp)
                Text(record.note?.takeIf { it.isNotBlank() } ?: record.category,
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(formatVnd(record.amountVnd, record.type, locale),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (record.type == TransactionType.INCOME) ExpenseManagerColors.income else ExpenseManagerColors.expense)
                Surface(color = if (record.type == TransactionType.INCOME) ExpenseManagerColors.incomeContainer
                    else ExpenseManagerColors.expenseContainer, shape = RoundedCornerShape(100.dp)) {
                    Text(stringResource(if (record.type == TransactionType.INCOME) R.string.type_income else R.string.type_expense),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (record.type == TransactionType.INCOME) ExpenseManagerColors.income else ExpenseManagerColors.expense)
                }
                Spacer(Modifier.height(8.dp))
                Surface(Modifier.fillMaxWidth(), color = ExpenseManagerColors.surface, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        DetailField(stringResource(R.string.transaction_category), record.category)
                        HorizontalDivider(color = ExpenseManagerColors.outline)
                        DetailField(stringResource(R.string.transaction_date), formatDate(record.date, locale))
                        walletLabel?.let {
                            HorizontalDivider(color = ExpenseManagerColors.outline)
                            DetailField(stringResource(R.string.wallet_label), it)
                        }
                    }
                }
                if (!record.note.isNullOrBlank()) {
                    Surface(Modifier.fillMaxWidth(), color = ExpenseManagerColors.surface, shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.transaction_note), style = MaterialTheme.typography.bodySmall,
                                color = ExpenseManagerColors.textSecondary)
                            Spacer(Modifier.height(8.dp))
                            Text(record.note, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (walletLoadFailed) ErrorBanner(stringResource(R.string.wallet_read_error), onRetryWallets)
                if (state.loadStatus == LoadStatus.ERROR) ErrorBanner(stringResource(R.string.load_transactions_error), onRetry)
                when (state.actionError) {
                    ActionError.DELETE_FAILED -> ErrorBanner(stringResource(R.string.delete_failed), { confirmDelete = true })
                    ActionError.RECORD_UNAVAILABLE -> ErrorBanner(stringResource(R.string.record_unavailable), onBack)
                    else -> Unit
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).height(50.dp).testTag("edit_transaction_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseManagerColors.primaryContainer,
                            contentColor = ExpenseManagerColors.primaryText),
                    ) {
                        Image(painterResource(R.drawable.ic_pencil), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.edit_transaction))
                    }
                    Button(
                        onClick = { confirmDelete = true },
                        enabled = !state.isDeleting,
                        modifier = Modifier.weight(1f).height(50.dp).testTag("delete_transaction_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseManagerColors.expenseContainer,
                            contentColor = ExpenseManagerColors.expense),
                    ) {
                        Image(painterResource(R.drawable.ic_trash), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(if (state.isDeleting) R.string.deleting_transaction else R.string.delete_transaction))
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }, modifier = Modifier.testTag("confirm_delete_button")) {
                    Text(stringResource(R.string.confirm_delete), color = ExpenseManagerColors.expense)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
            containerColor = ExpenseManagerColors.surface,
        )
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Row(Modifier.fillMaxWidth().heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = ExpenseManagerColors.textSecondary)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}
