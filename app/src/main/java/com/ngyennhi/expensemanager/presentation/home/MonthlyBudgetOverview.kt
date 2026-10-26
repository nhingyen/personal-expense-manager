package com.ngyennhi.expensemanager.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.MonthlyBudget
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Figma's monthly total card, fed by the same snapshot as the Dashboard. */
@Composable
internal fun MonthlyBudgetOverview(state: HomeUiState, locale: Locale) {
    if (state.budgetLoadStatus != HomeLoadStatus.READY) return
    Surface(color = ExpenseManagerColors.primaryContainer, shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().testTag("budget_overview")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_monthly_budget), Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium, color = ExpenseManagerColors.primaryText)
                state.budget?.let {
                    Text(stringResource(R.string.home_budget_used, budgetPercent(it, locale)),
                        modifier = Modifier.widthIn(max = 160.dp).testTag("budget_overview_percent"),
                        style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.primaryText)
                }
            }
            val limit = state.budgetLimitVnd
            if (limit == null) {
                Text(stringResource(R.string.home_budget_unset), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(homeVnd(limit, locale), modifier = Modifier.testTag("budget_overview_limit"),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 35.1.sp),
                    fontWeight = FontWeight.SemiBold, color = ExpenseManagerColors.primaryText)
            }
            state.budget?.let { BudgetProgress(it, "budget_overview_progress") }
            val summary = state.summary?.takeIf { state.canShowData }
            if (summary != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BudgetAmount(stringResource(R.string.budget_spent), summary.expenseVnd,
                        locale, Modifier.weight(1f).testTag("budget_overview_spent"))
                    state.budget?.let {
                        BudgetAmount(stringResource(R.string.budget_remaining), it.remainingVnd,
                            locale, Modifier.weight(1f).testTag("budget_overview_remaining"), remaining = true)
                    }
                }
                if (summary.expenseVnd == 0L) Text(stringResource(R.string.budget_no_expenses),
                    style = MaterialTheme.typography.bodySmall)
            } else {
                Text(stringResource(R.string.home_budget_totals_unavailable), style = MaterialTheme.typography.bodySmall)
            }
            state.budget?.takeIf { it.limitReached }?.let { BudgetWarning(it, locale) }
        }
    }
}

@Composable
private fun BudgetAmount(label: String, amount: Long, locale: Locale, modifier: Modifier, remaining: Boolean = false) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = if (remaining) Alignment.End else Alignment.Start) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ExpenseManagerColors.textSecondary)
        Text(homeVnd(amount, locale), style = MaterialTheme.typography.labelMedium,
            textAlign = if (remaining) TextAlign.End else TextAlign.Start,
            color = when {
                amount < 0 -> ExpenseManagerColors.expense
                remaining -> ExpenseManagerColors.income
                else -> ExpenseManagerColors.textPrimary
            })
    }
}

internal fun budgetPercent(budget: MonthlyBudget, locale: Locale): String =
    NumberFormat.getPercentInstance(locale).format(BigDecimal(budget.usedPercent, 2))

@Composable
internal fun BudgetProgress(budget: MonthlyBudget, tag: String) {
    LinearProgressIndicator(
        progress = { (budget.spentVnd.toDouble() / budget.limitVnd).coerceIn(0.0, 1.0).toFloat() },
        modifier = Modifier.fillMaxWidth().height(7.dp).testTag(tag),
        color = if (budget.remainingVnd < 0) ExpenseManagerColors.expense else ExpenseManagerColors.primary,
        trackColor = ExpenseManagerColors.mutedSurface, gapSize = 0.dp, drawStopIndicator = {},
    )
}

@Composable
internal fun BudgetWarning(budget: MonthlyBudget, locale: Locale, tag: String = "budget_warning") {
    Text(if (budget.remainingVnd < 0) stringResource(R.string.home_budget_exceeded, homeVnd(-budget.remainingVnd, locale))
        else stringResource(R.string.home_budget_reached),
        style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.expense,
        modifier = Modifier.testTag(tag).semantics { liveRegion = LiveRegionMode.Polite })
}
