package com.ngyennhi.expensemanager.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun AccountCard(state: AuthUiState, onLogin: () -> Unit, onLogout: () -> Unit, onRetry: () -> Unit) {
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(20.dp), color = ExpenseManagerColors.primaryContainer,
        modifier = Modifier.fillMaxWidth().testTag("settings_account")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.auth_account), style = MaterialTheme.typography.titleMedium)
            if (!state.isInitialized || state.isSubmitting) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(state.user?.email ?: stringResource(if (state.user == null) R.string.auth_guest_mode else R.string.auth_signed_in),
                Modifier.testTag("auth_account_status"), style = MaterialTheme.typography.bodyMedium)
            LocalDataNotice()
            if (state.sessionError) ErrorBanner(stringResource(R.string.auth_session_error), onRetry)
            state.error?.let { ErrorBanner(stringResource(authErrorMessage(it))) }
            if (state.user == null) {
                Button(onClick = onLogin, enabled = state.isInitialized && !state.isSubmitting && !state.sessionError,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("settings_login")) {
                    Text(stringResource(R.string.auth_login_or_register))
                }
            } else {
                OutlinedButton(onClick = { confirmLogout = true }, enabled = !state.isSubmitting,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("settings_logout")) {
                    Text(stringResource(R.string.auth_logout))
                }
            }
        }
    }
    if (confirmLogout && state.user != null) {
        AlertDialog(onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.auth_logout)) },
            text = { Text(stringResource(R.string.auth_logout_notice)) },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() },
                modifier = Modifier.testTag("auth_confirm_logout")) { Text(stringResource(R.string.auth_logout)) } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.cancel)) } })
    }
}
