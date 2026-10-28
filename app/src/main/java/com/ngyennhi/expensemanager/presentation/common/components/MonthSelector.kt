package com.ngyennhi.expensemanager.presentation.common.components

import com.ngyennhi.expensemanager.ui.theme.appearanceIconFilter
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R

@Composable
internal fun MonthSelector(label: String, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onPreviousMonth, enabled = enabled, modifier = Modifier.size(48.dp).testTag("month_previous")) {
            Image(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.previous_month), Modifier.size(20.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.textSecondary))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            Image(painterResource(R.drawable.ic_month_calendar), null, Modifier.size(18.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.textSecondary))
            Text(label, modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center)
        }
        IconButton(onClick = onNextMonth, enabled = enabled, modifier = Modifier.size(48.dp).testTag("month_next")) {
            Image(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.next_month), Modifier.size(20.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.textSecondary))
        }
    }
}
