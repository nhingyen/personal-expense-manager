package com.ngyennhi.expensemanager.presentation.wallet

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun SettingsWalletScreen(onWallets: () -> Unit) {
    Scaffold(containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(R.string.settings_title)) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Surface(shape = RoundedCornerShape(20.dp), color = ExpenseManagerColors.surface,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onWallets).testTag("settings_wallets")) {
                Row(Modifier.heightIn(min = 64.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(R.drawable.ic_wallet), null, Modifier.size(22.dp))
                    Text(stringResource(R.string.wallets_title), Modifier.weight(1f))
                    Image(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(18.dp))
                }
            }
        }
    }
}
