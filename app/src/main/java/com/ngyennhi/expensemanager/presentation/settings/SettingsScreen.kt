package com.ngyennhi.expensemanager.presentation.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    onLanguage: (AppLanguage) -> Unit,
    onTheme: (AppTheme) -> Unit,
    onRetry: () -> Unit,
    onWallets: () -> Unit,
    authState: com.ngyennhi.expensemanager.presentation.auth.AuthUiState = com.ngyennhi.expensemanager.presentation.auth.AuthUiState(isInitialized = true),
    onLogin: () -> Unit = {},
    onLogout: () -> Unit = {},
    onRetryAuth: () -> Unit = {},
) {
    val enabled = !state.isLoading && !state.isSaving && !state.readError
    Scaffold(containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(R.string.settings_title)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("settings_screen")
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.isLoading || state.isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.readError) ErrorBanner(stringResource(R.string.settings_read_error), onRetry)
            if (state.writeError) ErrorBanner(stringResource(R.string.settings_write_error))
            com.ngyennhi.expensemanager.presentation.auth.AccountCard(authState, onLogin, onLogout, onRetryAuth)
            SettingsCard {
                SettingsHeading(R.drawable.ic_settings_languages, stringResource(R.string.settings_language))
                // System fills a row; the two explicit languages retain the reference's equal columns.
                LanguageOption(AppLanguage.SYSTEM, state, enabled, onLanguage, Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp), maxItemsInEachRow = 2,
                    modifier = Modifier.fillMaxWidth().selectableGroup()) {
                    listOf(AppLanguage.VIETNAMESE, AppLanguage.ENGLISH).forEach { language ->
                        LanguageOption(language, state, enabled, onLanguage, Modifier.weight(1f))
                    }
                }
            }
            Surface(onClick = onWallets, shape = RoundedCornerShape(20.dp), color = ExpenseManagerColors.surface,
                modifier = Modifier.fillMaxWidth().testTag("settings_wallets")) {
                Row(Modifier.heightIn(min = 72.dp).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(painterResource(R.drawable.ic_wallet), null, Modifier.size(20.dp), tint = ExpenseManagerColors.primary)
                    Text(stringResource(R.string.wallets_title), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(16.dp), tint = ExpenseManagerColors.textSecondary)
                }
            }
            SettingsCard {
                SettingsHeading(R.drawable.ic_settings_palette, stringResource(R.string.settings_appearance))
                FlowRow(Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM).forEach { theme ->
                        val selected = state.theme == theme
                        Surface(shape = RoundedCornerShape(100.dp),
                            color = if (selected) ExpenseManagerColors.primaryContainer else ExpenseManagerColors.surface,
                            border = BorderStroke(1.dp, if (selected) ExpenseManagerColors.primaryContainer else ExpenseManagerColors.outline)) {
                            Box(Modifier.heightIn(min = 48.dp).widthIn(min = 84.dp)
                                .testTag("theme_${theme.name}")
                                .selectable(selected, enabled, Role.RadioButton) { onTheme(theme) }
                                .padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(when (theme) {
                                    AppTheme.LIGHT -> R.string.settings_light
                                    AppTheme.DARK -> R.string.settings_dark
                                    AppTheme.SYSTEM -> R.string.settings_system
                                }), style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) ExpenseManagerColors.primaryText else ExpenseManagerColors.textSecondary)
                            }
                        }
                    }
                }
            }
            SettingsCard {
                SettingsHeading(R.drawable.ic_settings_about, stringResource(R.string.settings_about))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.settings_about_description), style = MaterialTheme.typography.bodySmall,
                    color = ExpenseManagerColors.textSecondary)
                val context = LocalContext.current
                val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
                Text(stringResource(R.string.settings_version, version), Modifier.testTag("settings_version"),
                    style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.textSecondary)
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = ExpenseManagerColors.surface,
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun SettingsHeading(@DrawableRes icon: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = ExpenseManagerColors.primary)
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LanguageOption(language: AppLanguage, state: SettingsUiState, enabled: Boolean,
    onLanguage: (AppLanguage) -> Unit, modifier: Modifier = Modifier) {
    val selected = state.language == language
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp),
        color = if (selected) ExpenseManagerColors.primaryContainer else ExpenseManagerColors.surface,
        border = BorderStroke(1.dp, if (selected) ExpenseManagerColors.primary else ExpenseManagerColors.outline)) {
        Row(Modifier.heightIn(min = 48.dp).testTag("language_${language.name}")
            .selectable(selected, enabled, Role.RadioButton) { onLanguage(language) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(selected, onClick = null, enabled = enabled, modifier = Modifier.size(16.dp))
            Text(stringResource(when (language) {
                AppLanguage.SYSTEM -> R.string.settings_system_default
                AppLanguage.VIETNAMESE -> R.string.settings_vietnamese
                AppLanguage.ENGLISH -> R.string.settings_english
            }), style = MaterialTheme.typography.labelMedium,
                color = if (selected) ExpenseManagerColors.primaryText else ExpenseManagerColors.textSecondary)
        }
    }
}
