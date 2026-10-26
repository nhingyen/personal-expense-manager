package com.ngyennhi.expensemanager.presentation.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.ui.theme.appearanceIconFilter
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.util.Locale

@Composable
internal fun TypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.heightIn(min = 48.dp).semantics { this.selected = selected }.clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp)
                .background(
                    if (selected) ExpenseManagerColors.primaryContainer else ExpenseManagerColors.surface,
                    RoundedCornerShape(100.dp),
                )
                .border(1.dp, if (selected) ExpenseManagerColors.primaryContainer else ExpenseManagerColors.outline,
                    RoundedCornerShape(100.dp)).padding(horizontal = 8.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) ExpenseManagerColors.primaryText else ExpenseManagerColors.textSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
internal fun CategoryIcon(category: String, type: TransactionType, size: Dp = 44.dp) {
    val normalized = category.trim().lowercase(Locale.ROOT)
    val icon = when {
        normalized.contains("ăn uống") || normalized.contains("food") -> R.drawable.ic_utensils
        normalized.contains("cà phê") || normalized.contains("coffee") -> R.drawable.ic_coffee
        normalized.contains("mua sắm") || normalized.contains("shopping") -> R.drawable.ic_shopping_bag
        normalized.contains("di chuyển") || normalized.contains("transport") -> R.drawable.ic_car
        normalized.contains("lương") || normalized.contains("salary") -> R.drawable.ic_briefcase
        else -> null
    }
    val background = when (icon) {
        R.drawable.ic_coffee -> ExpenseManagerColors.yellowContainer
        R.drawable.ic_shopping_bag -> ExpenseManagerColors.primaryContainer
        R.drawable.ic_car, R.drawable.ic_briefcase -> ExpenseManagerColors.incomeContainer
        else -> if (type == TransactionType.INCOME) ExpenseManagerColors.incomeContainer else ExpenseManagerColors.expenseContainer
    }
    Box(
        modifier = Modifier.size(size).background(background, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp), colorFilter = appearanceIconFilter(if (type == TransactionType.INCOME) ExpenseManagerColors.income else ExpenseManagerColors.expense))
        } else {
            Text(category.trim().take(1).uppercase(Locale.ROOT), style = MaterialTheme.typography.titleMedium,
                color = if (type == TransactionType.INCOME) ExpenseManagerColors.income else ExpenseManagerColors.expense)
        }
    }
}

@Composable
internal fun ErrorBanner(message: String, onRetry: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        color = ExpenseManagerColors.expenseContainer,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = ExpenseManagerColors.expense)
            if (onRetry != null) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
internal fun UnavailableState(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.record_unavailable), style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) { Text(stringResource(R.string.back_to_transactions)) }
    }
}

@Composable
internal fun TransactionRow(record: Transaction, locale: Locale, onClick: () -> Unit) {
    val type = stringResource(if (record.type == TransactionType.INCOME) R.string.type_income else R.string.type_expense)
    val hasNote = !record.note.isNullOrBlank()
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("transaction_row_${record.id}")
            .semantics { stateDescription = type }.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(record.category, record.type)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(if (hasNote) record.note.orEmpty() else record.category, style = MaterialTheme.typography.labelLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val date = formatDate(record.date, locale)
            val summary = stringResource(R.string.transaction_row_summary, if (hasNote) record.category else type, date)
            Text(summary, style = MaterialTheme.typography.bodySmall,
                color = ExpenseManagerColors.textSecondary,
                maxLines = if (LocalConfiguration.current.fontScale > 1f) 2 else 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatVnd(record.amountVnd, record.type, locale),
            modifier = Modifier.widthIn(max = 160.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (record.type == TransactionType.INCOME) ExpenseManagerColors.income else ExpenseManagerColors.textPrimary)
    }
}
