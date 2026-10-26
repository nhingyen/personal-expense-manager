package com.ngyennhi.expensemanager.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
internal fun MonthlySummaryCard(summary: MonthlySummary, locale: Locale) {
    val currencyFormat = NumberFormat.getCurrencyInstance(locale).apply {
        currency = Currency.getInstance("VND")
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    val month = summary.month.format(DateTimeFormatter.ofPattern("MMMM", locale))
    val incomeLabel = stringResource(R.string.home_monthly_income, month)
    val expenseLabel = stringResource(R.string.home_monthly_expense, month)
    val income = currencyFormat.format(summary.incomeVnd)
    val expense = currencyFormat.format(summary.expenseVnd)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("home_summary"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ExpenseManagerColors.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.home_net_income),
                    modifier = Modifier.heightIn(min = 22.dp),
                    style = MaterialTheme.typography.bodyMedium, color = ExpenseManagerColors.primaryText)
                Text(currencyFormat.format(summary.netVnd), style = MaterialTheme.typography.headlineLarge,
                    color = ExpenseManagerColors.primaryText, modifier = Modifier.testTag("home_net"))
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val amountStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                val labelStyle = MaterialTheme.typography.bodySmall
                val columnWidth = with(density) { ((maxWidth - 12.dp) / 2).toPx() }
                val iconAndGap = with(density) { 21.dp.toPx() }
                // Match Figma's two columns whenever the actual localized text fits.
                // Fall back to full-width totals for large values or enlarged fonts.
                val sideBySide = listOf(income, expense).all {
                    measurer.measure(it, amountStyle).size.width <= columnWidth
                } && listOf(incomeLabel, expenseLabel).all {
                    measurer.measure(it, labelStyle).size.width + iconAndGap <= columnWidth
                }
                val incomeContent: @Composable (Modifier) -> Unit = { modifier ->
                    SummaryTotal(incomeLabel, income, R.drawable.ic_summary_income,
                        ExpenseManagerColors.income, "home_income", modifier)
                }
                val expenseContent: @Composable (Modifier) -> Unit = { modifier ->
                    SummaryTotal(expenseLabel, expense, R.drawable.ic_summary_expense,
                        ExpenseManagerColors.expense, "home_expense", modifier)
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
    }
}

@Composable
private fun SummaryTotal(label: String, amount: String, icon: Int, color: Color, tag: String, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Image(painterResource(icon), null, Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        }
        Text(amount, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
            color = ExpenseManagerColors.textPrimary, modifier = Modifier.testTag(tag))
    }
}