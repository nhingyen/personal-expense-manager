package com.ngyennhi.expensemanager.presentation.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.MonthlyStatistics
import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.presentation.transaction.formatMonth
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

private val categoryColors = listOf(ExpenseManagerColors.chartCoral, ExpenseManagerColors.chartPurple,
    ExpenseManagerColors.chartGreen, ExpenseManagerColors.chartYellow)

@Composable
internal fun CategoryExpenseCard(statistics: MonthlyStatistics, locale: Locale) {
    StatisticsCard(stringResource(R.string.statistics_categories), "statistics_categories") {
        if (statistics.categories.isEmpty()) {
            Text(stringResource(R.string.statistics_no_expenses), style = MaterialTheme.typography.bodyMedium,
                color = ExpenseManagerColors.textSecondary, modifier = Modifier.testTag("statistics_no_expenses"))
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val horizontal = maxWidth >= 300.dp && LocalDensity.current.fontScale <= 1.15f
                if (horizontal) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExpenseDonut(statistics, locale)
                        CategoryLegend(statistics, locale, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        ExpenseDonut(statistics, locale)
                        CategoryLegend(statistics, locale, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseDonut(statistics: MonthlyStatistics, locale: Locale) {
    // The graph is decorative: the total and every slice have exact text equivalents.
    Box(Modifier.size(140.dp).testTag("statistics_donut"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(140.dp).clearAndSetSemantics {}) {
            val stroke = 16.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var start = -90f
            statistics.categories.forEachIndexed { index, category ->
                val sweep = (category.share * 360.0).toFloat()
                drawArc(categoryColors[index % categoryColors.size], start, sweep, false,
                    Offset(stroke / 2, stroke / 2), arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        Column(Modifier.width(100.dp).clearAndSetSemantics {}, horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.statistics_total_expense), style = MaterialTheme.typography.labelSmall,
                color = ExpenseManagerColors.textSecondary, textAlign = TextAlign.Center)
            val total = statistics.summary.expenseVnd
            val amount = if (total >= 1_000_000) {
                val millions = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }
                    .format(total.toDouble() / 1_000_000)
                stringResource(R.string.statistics_millions_vnd, millions)
            } else statisticsCurrency(total, locale)
            val measurer = rememberTextMeasurer()
            val available = with(LocalDensity.current) { 100.dp.toPx() }
            // Long amounts remain exact in the summary/legend; keep the ring's center readable.
            if (measurer.measure(amount, MaterialTheme.typography.bodyLarge).size.width <= available) {
                Text(amount, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun CategoryLegend(statistics: MonthlyStatistics, locale: Locale, modifier: Modifier) {
    val percentage = NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        statistics.categories.forEachIndexed { index, category ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {}) {
                ChartSwatch(categoryColors[index % categoryColors.size])
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(category.category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.statistics_category_value,
                        percentage.format(category.share), statisticsCurrency(category.amountVnd, locale)),
                        style = MaterialTheme.typography.labelSmall, color = ExpenseManagerColors.textSecondary)
                }
            }
        }
    }
}

@Composable
internal fun MonthlyComparisonCard(months: List<MonthlySummary>, locale: Locale) {
    StatisticsCard(stringResource(R.string.statistics_comparison), "statistics_comparison") {
        Text(stringResource(R.string.statistics_month_range,
            formatMonth(months.first().month, locale), formatMonth(months.last().month, locale)),
            style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChartLegend(ExpenseManagerColors.chartGreen, stringResource(R.string.type_income))
            ChartLegend(ExpenseManagerColors.chartCoral, stringResource(R.string.type_expense))
        }
        val maximum = months.maxOf { maxOf(it.incomeVnd, it.expenseVnd) }
        if (maximum == 0L) {
            Text(stringResource(R.string.statistics_no_comparison), style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("statistics_no_comparison"))
        } else {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val minimumGroupWidth = 40.dp * LocalDensity.current.fontScale
                val groupWidth = maxOf(minimumGroupWidth, (maxWidth - 12.dp * (months.size - 1)) / months.size)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clearAndSetSemantics {},
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    months.forEach { month ->
                        Column(Modifier.width(groupWidth), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.height(112.dp), verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                ComparisonBar(month.incomeVnd, maximum, ExpenseManagerColors.chartGreen)
                                ComparisonBar(month.expenseVnd, maximum, ExpenseManagerColors.chartCoral)
                            }
                            Text(month.month.format(DateTimeFormatter.ofPattern("MM/yy", locale)),
                                style = MaterialTheme.typography.labelSmall, color = ExpenseManagerColors.textSecondary,
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        // Exact values remain readable and accessible without interpreting bar height/color.
        months.forEach { month ->
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formatMonth(month.month, locale), style = MaterialTheme.typography.labelMedium)
                Text(stringResource(R.string.statistics_month_income, statisticsCurrency(month.incomeVnd, locale)),
                    style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.income)
                Text(stringResource(R.string.statistics_month_expense, statisticsCurrency(month.expenseVnd, locale)),
                    style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.expense)
            }
        }
    }
}

@Composable
private fun ComparisonBar(amount: Long, maximum: Long, color: Color) {
    // Never fabricate a minimum positive height for zero; maximum is checked by the caller.
    val height = (112.0 * amount.toDouble() / maximum.toDouble()).toFloat().dp
    Spacer(Modifier.width(18.dp).height(height).background(color, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
}

@Composable
private fun ChartLegend(color: Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        ChartSwatch(color)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChartSwatch(color: Color) {
    Spacer(Modifier.size(8.dp).background(color, RoundedCornerShape(3.dp)))
}
