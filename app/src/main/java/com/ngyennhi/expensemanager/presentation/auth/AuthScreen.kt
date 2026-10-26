package com.ngyennhi.expensemanager.presentation.auth

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.AuthError
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

class AuthActions(
    val onOpen: () -> Unit = {},
    val onEmail: (String) -> Unit = {},
    val onRegistering: (Boolean) -> Unit = {},
    val onPasswordChanged: () -> Unit = {},
    val onSubmit: (String, String) -> Unit = { _, _ -> },
    val onGoogle: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onRetrySession: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)

@Composable
internal fun AuthScreen(state: AuthUiState, actions: AuthActions, onBack: () -> Unit) {
    // Intentionally not saveable: configuration recreation, leaving and process death clear secrets.
    var password by remember(state.registering) { mutableStateOf("") }
    var confirmation by remember(state.registering) { mutableStateOf("") }
    val enabled = state.isInitialized && !state.isSubmitting && !state.sessionError && state.user == null
    val submit: () -> Unit = { if (enabled) actions.onSubmit(password, confirmation) }
    BackHandler { if (!state.isSubmitting) onBack() }

    Scaffold(
        containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(R.string.auth_account), onBack = { if (!state.isSubmitting) onBack() }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().testTag("auth_screen")
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AuthWelcomeCard(state.registering)
            AuthModeSelector(state.registering, !state.isSubmitting, actions.onRegistering)

            if (!state.isInitialized || state.isSubmitting) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag("auth_loading"))
                    Text(stringResource(R.string.auth_working), style = MaterialTheme.typography.bodySmall,
                        color = ExpenseManagerColors.textSecondary)
                }
            }
            if (state.sessionError) ErrorBanner(stringResource(R.string.auth_session_error), actions.onRetrySession)
            state.error?.let { ErrorBanner(stringResource(authErrorMessage(it))) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AuthField(
                    value = state.email, onValueChange = actions.onEmail, enabled = enabled,
                    label = stringResource(R.string.auth_email), tag = "auth_email",
                    error = if (state.validation.invalidEmail) stringResource(R.string.auth_invalid_email) else null,
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Next,
                )
                val passwordError = when {
                    state.validation.missingPassword -> stringResource(R.string.auth_password_required)
                    state.validation.weakPassword -> stringResource(R.string.auth_password_minimum)
                    else -> null
                }
                AuthField(
                    value = password, onValueChange = { password = it; actions.onPasswordChanged() }, enabled = enabled,
                    label = stringResource(R.string.auth_password), tag = "auth_password", error = passwordError,
                    password = true,
                    hint = if (state.registering) stringResource(R.string.auth_password_hint) else null,
                    imeAction = if (state.registering) ImeAction.Next else ImeAction.Done, onDone = submit,
                )
                if (state.registering) {
                    AuthField(
                        value = confirmation, onValueChange = { confirmation = it; actions.onPasswordChanged() },
                        enabled = enabled, label = stringResource(R.string.auth_confirm_password),
                        tag = "auth_confirmation", password = true,
                        error = if (state.validation.passwordMismatch) stringResource(R.string.auth_password_mismatch) else null,
                        imeAction = ImeAction.Done, onDone = submit,
                    )
                }
            }

            Button(
                onClick = submit, enabled = enabled, shape = RoundedCornerShape(100.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("auth_submit"),
            ) {
                Text(stringResource(if (state.registering) R.string.auth_register else R.string.auth_login),
                    style = MaterialTheme.typography.labelLarge)
            }
            if (!state.registering) {
                OutlinedButton(
                    onClick = { password = ""; confirmation = ""; actions.onGoogle() },
                    enabled = enabled, shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("auth_google"),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = ExpenseManagerColors.surface),
                ) {
                    Image(painterResource(R.drawable.google_g), null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.auth_continue_google), style = MaterialTheme.typography.labelLarge,
                        color = ExpenseManagerColors.textPrimary)
                }
            }
            OutlinedButton(
                onClick = onBack, enabled = !state.isSubmitting, shape = RoundedCornerShape(100.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("auth_guest"),
            ) {
                Text(stringResource(R.string.auth_continue_guest), style = MaterialTheme.typography.labelLarge)
            }
            AuthDeviceNotice()
        }
    }
}

@Composable
private fun AuthWelcomeCard(registering: Boolean) {
    Surface(shape = RoundedCornerShape(20.dp), color = ExpenseManagerColors.primaryContainer,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = ExpenseManagerColors.surface) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_wallet), null, Modifier.size(22.dp), tint = ExpenseManagerColors.primary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelSmall,
                    color = ExpenseManagerColors.primaryText)
                Text(stringResource(if (registering) R.string.auth_welcome_register else R.string.auth_welcome_login),
                    style = MaterialTheme.typography.titleLarge, color = ExpenseManagerColors.primaryText)
            }
        }
    }
}

@Composable
private fun AuthModeSelector(registering: Boolean, enabled: Boolean, onRegistering: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(false, true).forEach { mode ->
            val selected = registering == mode
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = if (selected) ExpenseManagerColors.primaryContainer else ExpenseManagerColors.surface,
                border = BorderStroke(1.dp, if (selected) ExpenseManagerColors.primaryContainer else ExpenseManagerColors.outline),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag(if (selected) "auth_selected_mode" else "auth_switch_mode")
                        .selectable(selected, enabled, Role.Tab) { onRegistering(mode) }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(if (mode) R.string.auth_register else R.string.auth_login),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) ExpenseManagerColors.primaryText else ExpenseManagerColors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    label: String,
    tag: String,
    error: String?,
    password: Boolean = false,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Password,
    imeAction: ImeAction,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, enabled = enabled,
        modifier = Modifier.fillMaxWidth().testTag(tag), singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        placeholder = hint?.let { { Text(it, style = MaterialTheme.typography.bodyMedium) } },
        isError = error != null,
        supportingText = error?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ExpenseManagerColors.surface,
            unfocusedContainerColor = ExpenseManagerColors.surface,
            disabledContainerColor = ExpenseManagerColors.surface,
            errorContainerColor = ExpenseManagerColors.surface,
        ),
    )
}

@Composable
private fun AuthDeviceNotice() {
    Surface(shape = RoundedCornerShape(14.dp), color = ExpenseManagerColors.mutedSurface,
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_wallet), null, Modifier.padding(top = 2.dp).size(18.dp),
                tint = ExpenseManagerColors.primary)
            Text(stringResource(R.string.auth_local_data_summary), Modifier.weight(1f).testTag("auth_local_data_notice"),
                style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.textPrimary)
        }
    }
}

@Composable
internal fun LocalDataNotice() {
    Text(stringResource(R.string.auth_local_data_notice), Modifier.testTag("auth_local_data_notice"),
        style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.textSecondary)
}

@StringRes
internal fun authErrorMessage(error: AuthError): Int = when (error) {
    AuthError.INVALID_CREDENTIALS -> R.string.auth_invalid_credentials
    AuthError.EMAIL_IN_USE -> R.string.auth_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_weak_password
    AuthError.NETWORK -> R.string.auth_network_error
    AuthError.TOO_MANY_REQUESTS -> R.string.auth_too_many_requests
    AuthError.USER_DISABLED -> R.string.auth_user_disabled
    AuthError.NOT_CONFIGURED -> R.string.auth_not_configured
    AuthError.PROVIDER_CONFLICT -> R.string.auth_provider_conflict
    AuthError.GOOGLE_UNAVAILABLE -> R.string.auth_google_unavailable
    AuthError.GOOGLE_FAILED -> R.string.auth_google_failed
    AuthError.UNKNOWN -> R.string.auth_unknown_error
}
