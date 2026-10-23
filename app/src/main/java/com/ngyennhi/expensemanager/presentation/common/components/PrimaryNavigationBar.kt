package com.ngyennhi.expensemanager.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

@Composable
internal fun PrimaryNavigationBar(
    isHome: Boolean,
    onHome: () -> Unit,
    onTransactions: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(color = ExpenseManagerColors.surface) {
        Column {
            HorizontalDivider(color = ExpenseManagerColors.outline)
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).testTag("primary_navigation")
                    .heightIn(min = 80.dp).padding(horizontal = 8.dp).selectableGroup(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryNavigationItem(stringResource(R.string.dashboard_title), R.drawable.ic_home,
                    isHome, onHome, Modifier.weight(1f).testTag("nav_home"))
                PrimaryNavigationItem(stringResource(R.string.navigation_add), R.drawable.ic_navigation_add,
                    false, onAdd, Modifier.weight(1f).testTag("nav_add"), isAction = true)
                PrimaryNavigationItem(stringResource(R.string.transactions_title), R.drawable.ic_transactions,
                    !isHome, onTransactions, Modifier.weight(1f).testTag("nav_transactions"))
            }
        }
    }
}

@Composable
private fun PrimaryNavigationItem(
    label: String,
    icon: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    isAction: Boolean = false,
) {
    val color = if (selected) ExpenseManagerColors.primary else ExpenseManagerColors.textSecondary
    Column(
        modifier = modifier.heightIn(min = 64.dp).then(
            if (isAction) Modifier.clickable(role = Role.Button, onClick = onClick)
            else Modifier.selectable(selected = selected, role = Role.Tab, onClick = onClick)
        ).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            Modifier.size(width = if (isAction) 48.dp else 58.dp, height = 32.dp)
                .background(when {
                    isAction -> ExpenseManagerColors.primary
                    selected -> ExpenseManagerColors.primaryContainer
                    else -> ExpenseManagerColors.surface
                }, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, Modifier.size(22.dp),
                tint = if (isAction) ExpenseManagerColors.surface else color)
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color, textAlign = TextAlign.Center)
    }
}
