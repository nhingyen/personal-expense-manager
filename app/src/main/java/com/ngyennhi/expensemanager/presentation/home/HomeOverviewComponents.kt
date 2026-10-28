package com.ngyennhi.expensemanager.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.ui.theme.appearanceIconFilter
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
internal fun HomeGreeting(dateTime: LocalDateTime, month: YearMonth, locale: Locale, onSelectMonth: () -> Unit) {
    val greeting = when (dateTime.hour) {
        in 0..11 -> R.string.home_good_morning
        in 12..17 -> R.string.home_good_afternoon
        else -> R.string.home_good_evening
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 47.dp).testTag("home_greeting"),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val monthDescription = stringResource(R.string.home_select_month,
            com.ngyennhi.expensemanager.presentation.transaction.formatMonth(month, locale))
        Column(Modifier.weight(1f).heightIn(min = 48.dp).testTag("home_month_picker")
            .clickable(role = Role.Button, onClick = onSelectMonth).semantics { contentDescription = monthDescription },
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
            val date = (if (YearMonth.from(dateTime) == month) dateTime.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
                else com.ngyennhi.expensemanager.presentation.transaction.formatMonth(month, locale))
                .replaceFirstChar { it.titlecase(locale) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(date, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Normal,
                    color = ExpenseManagerColors.textSecondary, modifier = Modifier.testTag("home_today"))
                Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(12.dp).rotate(90f),
                    tint = ExpenseManagerColors.textSecondary)
            }
            Text(stringResource(greeting), style = MaterialTheme.typography.titleLarge)
        }
        Box(Modifier.size(42.dp).background(ExpenseManagerColors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_home_avatar), null, Modifier.size(22.dp),
                tint = ExpenseManagerColors.primary)
        }
    }
}

@Composable
internal fun HomeShortcuts(onBudget: () -> Unit, onCategories: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag("home_shortcuts"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeShortcut(stringResource(R.string.home_budget), R.drawable.ic_home_budget,
            ExpenseManagerColors.incomeContainer, onBudget, Modifier.weight(1f).testTag("home_budget_shortcut"))
        HomeShortcut(stringResource(R.string.home_categories), R.drawable.ic_home_categories,
            ExpenseManagerColors.yellowContainer, onCategories, Modifier.weight(1f).testTag("home_categories_shortcut"))
    }
}

@Composable
private fun HomeShortcut(label: String, icon: Int, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, color = color, shape = RoundedCornerShape(14.dp), modifier = modifier.heightIn(min = 48.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), null, Modifier.size(20.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.primary))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

internal fun homeVnd(amount: Long, locale: Locale): String = NumberFormat.getCurrencyInstance(locale).apply {
    currency = Currency.getInstance("VND")
    minimumFractionDigits = 0
    maximumFractionDigits = 0
}.format(amount)

@Composable
internal fun HomeBudgetCard(state: HomeUiState, locale: Locale, onOpen: () -> Unit, onRetry: () -> Unit) {
    Surface(onClick = onOpen, color = ExpenseManagerColors.surface, shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().testTag("home_budget_card")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.home_monthly_budget), Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.labelLarge)
                if (state.budgetLoadStatus == HomeLoadStatus.READY && state.budgetLimitVnd == null) {
                    Text(stringResource(R.string.home_set_budget), style = MaterialTheme.typography.bodySmall,
                        color = ExpenseManagerColors.primary)
                }
                state.budget?.let { budget ->
                    val percent = budgetPercent(budget, locale)
                    Text(stringResource(R.string.home_budget_used, percent), style = MaterialTheme.typography.bodySmall,
                        color = ExpenseManagerColors.primary,
                        modifier = Modifier.widthIn(max = 160.dp).testTag("home_budget_percentage"))
                }
            }
            when {
                state.budgetLoadStatus == HomeLoadStatus.LOADING ->
                    Text(stringResource(R.string.home_budget_loading), style = MaterialTheme.typography.bodySmall)
                state.budgetLoadStatus == HomeLoadStatus.ERROR ->
                    ErrorBanner(stringResource(R.string.home_budget_load_error), onRetry)
                state.budgetLimitVnd == null ->
                    Text(stringResource(R.string.home_budget_unset), style = MaterialTheme.typography.bodySmall,
                        color = ExpenseManagerColors.textSecondary)
                state.budget == null ->
                    Text(stringResource(R.string.home_budget_totals_unavailable), style = MaterialTheme.typography.bodySmall,
                        color = ExpenseManagerColors.textSecondary)
                else -> {
                    val budget = state.budget
                    BudgetProgress(budget, "home_budget_progress")
                    if (budget.limitReached) {
                        BudgetWarning(budget, locale, "home_budget_warning")
                    } else {
                        Text(stringResource(R.string.home_budget_remaining, homeVnd(budget.remainingVnd, locale)),
                            style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.textSecondary,
                            modifier = Modifier.testTag("home_budget_remaining"))
                    }
                }
            }
        }
    }
}
