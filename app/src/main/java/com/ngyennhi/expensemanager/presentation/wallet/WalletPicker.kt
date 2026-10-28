package com.ngyennhi.expensemanager.presentation.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.ui.theme.appearanceIconFilter
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun WalletPicker(state: WalletUiState, selectedId: Long, enabled: Boolean, onSelect: (Long) -> Unit, onRetry: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    when (state.status) {
        LoadStatus.LOADING -> CircularProgressIndicator(Modifier.size(24.dp))
        LoadStatus.ERROR -> ErrorBanner(stringResource(R.string.wallet_read_error), onRetry)
        LoadStatus.READY -> Box {
            val wallet = state.wallets.firstOrNull { it.id == selectedId }
            Surface(color = ExpenseManagerColors.surface, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("wallet_picker")
                    .clickable(enabled = enabled) { expanded = true }) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(R.drawable.ic_wallet_form), null, Modifier.size(18.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.primary))
                    Text(stringResource(R.string.wallet_selected, wallet?.let { walletName(it) }
                        ?: stringResource(R.string.wallet_choose)), Modifier.weight(1f))
                    Image(painterResource(R.drawable.ic_chevron_down), null, Modifier.size(18.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.textSecondary))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.wallets.forEach { wallet -> DropdownMenuItem(text = { Text(walletName(wallet)) },
                    modifier = Modifier.testTag("wallet_option_${wallet.id}"),
                    onClick = { onSelect(wallet.id); expanded = false }) }
            }
        }
    }
}
