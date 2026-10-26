package com.ngyennhi.expensemanager.presentation.wallet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.WalletType
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.transaction.TypeChip
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun WalletFormScreen(state: WalletUiState, onBack: () -> Unit, onName: (String) -> Unit,
    onType: (WalletType) -> Unit, onOpening: (String) -> Unit, onSave: () -> Unit, onRetry: () -> Unit) {
    BackHandler(enabled = state.writing) { /* Keep the pending write attached to this screen. */ }
    val form = state.form
    Scaffold(containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(if (form.id == null) R.string.wallet_add else R.string.wallet_edit),
            onBack = { if (!state.writing) onBack() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WalletErrorBanner(state.error, onRetry)
            if (state.status == LoadStatus.LOADING) CircularProgressIndicator()
            OutlinedTextField(value = form.name, onValueChange = onName, label = { Text(stringResource(R.string.wallet_name)) },
                placeholder = { Text(stringResource(R.string.wallet_name_hint)) },
                isError = form.invalidName, enabled = !state.writing, singleLine = true,
                supportingText = { if (form.invalidName) Text(stringResource(R.string.wallet_name_invalid)) },
                modifier = Modifier.fillMaxWidth().testTag("wallet_name_input"))
            Text(stringResource(R.string.wallet_type), style = MaterialTheme.typography.labelLarge)
            // Stack choices so localized labels remain readable at large text sizes.
            WalletType.entries.forEach { type ->
                TypeChip(walletTypeName(type), form.type == type, { if (!state.writing) onType(type) },
                    Modifier.fillMaxWidth().testTag("wallet_type_${type.name}"))
            }
            OutlinedTextField(value = form.openingInput, onValueChange = onOpening,
                label = { Text(stringResource(R.string.wallet_opening)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = form.invalidOpening, enabled = !state.writing, singleLine = true,
                supportingText = { Text(stringResource(if (form.invalidOpening) R.string.wallet_opening_invalid else R.string.wallet_opening_hint)) },
                modifier = Modifier.fillMaxWidth().testTag("wallet_opening_input"))
            Text(stringResource(R.string.wallet_manual_hint), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onSave, enabled = !state.writing && state.status == LoadStatus.READY,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("wallet_save_button")) {
                Text(stringResource(if (state.writing) R.string.saving_transaction else R.string.wallet_save))
            }
            TextButton(onClick = onBack, enabled = !state.writing, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
        }
    }
}
