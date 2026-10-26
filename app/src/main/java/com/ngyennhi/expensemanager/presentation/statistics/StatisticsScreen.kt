package com.ngyennhi.expensemanager.presentation.statistics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.presentation.common.components.MonthSelector
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.presentation.transaction.formatMonth
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Scaffold(
        modifier = Modifier.testTag("statistics_screen"),
        containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(R.string.statistics_title)) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("statistics_content"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                MonthSelector(formatMonth(state.selectedMonth, locale), onPreviousMonth, onNextMonth)
            }
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.testTag("statistics_loading"))
                    }
                }
            }
            state.error?.let { error ->
                item {
                    ErrorBanner(stringResource(if (error == StatisticsError.CALCULATION_OVERFLOW)
                        R.string.statistics_overflow else R.string.statistics_load_error), onRetry)
                }
            }
            state.statistics?.let { statistics ->
                item { StatisticsTotals(statistics.summary, locale) }
                if (statistics.summary.incomeVnd == 0L && statistics.summary.expenseVnd == 0L) {
                    item {
                        Text(stringResource(R.string.statistics_empty_month),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("statistics_empty"))
                    }
                }
                item { CategoryExpenseCard(statistics, locale) }
                item { MonthlyComparisonCard(statistics.comparison, locale) }
            }
        }
    }
}

@Composable
private fun StatisticsTotals(summary: MonthlySummary, locale: Locale) {
    val income = statisticsCurrency(summary.incomeVnd, locale)
    val expense = statisticsCurrency(summary.expenseVnd, locale)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val amountStyle = MaterialTheme.typography.titleMedium
        val available = with(density) { ((maxWidth - 12.dp) / 2 - 24.dp).toPx() }
        val sideBySide = listOf(income, expense).all { measurer.measure(it, amountStyle).size.width <= available }
        val incomeContent: @Composable (Modifier) -> Unit = { modifier ->
            StatisticsTotal(stringResource(R.string.type_income), income, R.drawable.ic_summary_income,
                ExpenseManagerColors.incomeContainer, ExpenseManagerColors.income,
                modifier.testTag("statistics_income"))
        }
        val expenseContent: @Composable (Modifier) -> Unit = { modifier ->
            StatisticsTotal(stringResource(R.string.type_expense), expense, R.drawable.ic_summary_expense,
                ExpenseManagerColors.expenseContainer, ExpenseManagerColors.expense,
                modifier.testTag("statistics_expense"))
        }
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                incomeContent(Modifier.weight(1f))
                expenseContent(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                incomeContent(Modifier.fillMaxWidth())
                expenseContent(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StatisticsTotal(label: String, amount: String, icon: Int, background: Color, tint: Color, modifier: Modifier) {
    Card(modifier.semantics(mergeDescendants = true) {}, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(icon), null, Modifier.size(16.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = tint)
            }
            Text(amount, style = MaterialTheme.typography.titleMedium, color = ExpenseManagerColors.textPrimary)
        }
    }
}

@Composable
internal fun StatisticsCard(title: String, tag: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().testTag(tag), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ExpenseManagerColors.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

internal fun statisticsCurrency(amount: Long, locale: Locale): String =
    NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance("VND")
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }.format(amount)
