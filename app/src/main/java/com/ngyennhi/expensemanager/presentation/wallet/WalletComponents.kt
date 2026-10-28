package com.ngyennhi.expensemanager.presentation.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.ui.theme.appearanceIconFilter
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.text.NumberFormat

@Composable
internal fun walletName(wallet: Wallet): String = wallet.name.ifBlank { stringResource(R.string.wallet_default) }

@Composable
internal fun walletTypeName(type: WalletType): String = stringResource(when (type) {
    WalletType.CASH -> R.string.wallet_cash
    WalletType.BANK -> R.string.wallet_bank
    WalletType.E_WALLET -> R.string.wallet_electronic
})

@Composable
internal fun walletAmount(amount: Long): String =
    NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0]).format(amount) + stringResource(R.string.vnd_suffix)

@Composable
internal fun WalletIcon(type: WalletType) {
    Box(Modifier.size(44.dp).background(when (type) {
        WalletType.CASH -> ExpenseManagerColors.primaryContainer
        WalletType.BANK -> ExpenseManagerColors.bankContainer
        WalletType.E_WALLET -> ExpenseManagerColors.incomeContainer
    }, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Image(painterResource(if (type == WalletType.BANK) R.drawable.ic_wallet_bank else R.drawable.ic_wallet),
            null, Modifier.size(22.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.primary))
    }
}

@Composable
internal fun WalletBalanceCard(label: String, amount: Long, supporting: String) {
    Surface(color = ExpenseManagerColors.primaryContainer, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = ExpenseManagerColors.primaryText)
                Image(painterResource(R.drawable.ic_wallet), null, Modifier.size(22.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.primary))
            }
            Text(walletAmount(amount), style = MaterialTheme.typography.headlineLarge,
                color = ExpenseManagerColors.primaryText)
            Text(supporting, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal), color = ExpenseManagerColors.primaryText)
        }
    }
}

@Composable
internal fun WalletErrorBanner(error: WalletError?, onRetry: () -> Unit) {
    error?.let {
        ErrorBanner(stringResource(when (it) {
            WalletError.READ -> R.string.wallet_read_error
            WalletError.OVERFLOW -> R.string.wallet_overflow
            WalletError.WRITE -> R.string.wallet_write_error
            WalletError.DELETE_BLOCKED -> R.string.wallet_delete_blocked
        }), if (it == WalletError.READ) onRetry else null)
    }
}
