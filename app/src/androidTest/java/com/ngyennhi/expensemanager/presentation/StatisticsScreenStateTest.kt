package com.ngyennhi.expensemanager.presentation

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlyStatisticsUseCase
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlySummaryUseCase
import com.ngyennhi.expensemanager.presentation.statistics.StatisticsError
import com.ngyennhi.expensemanager.presentation.statistics.StatisticsScreen
import com.ngyennhi.expensemanager.presentation.statistics.StatisticsUiState
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import java.io.File
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Currency
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatisticsScreenStateTest {
    @get:Rule val composeRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val month = YearMonth.of(2026, 10)
    private val calculate = CalculateMonthlyStatisticsUseCase(CalculateMonthlySummaryUseCase())

    @Test
    fun loadingEmptyIncomeOnlyAndErrorStatesAndCallbacks() {
        var state by mutableStateOf(StatisticsUiState(month))
        var previous = 0
        var next = 0
        var retries = 0
        composeRule.setContent {
            PersonalExpenseManagerTheme {
                StatisticsScreen(state, { previous++ }, { next++ }, { retries++ })
            }
        }
        composeRule.onNodeWithTag("statistics_loading").assertExists()
        composeRule.onNodeWithTag("statistics_income").assertDoesNotExist()
        composeRule.onNodeWithTag("month_previous").performClick()
        composeRule.onNodeWithTag("month_next").performClick()
        assertEquals(1, previous)
        assertEquals(1, next)
        composeRule.runOnIdle { state = StatisticsUiState(month, false, calculate(emptyList(), month)) }
        composeRule.onNodeWithTag("statistics_loading").assertDoesNotExist()
        composeRule.onNodeWithTag("statistics_empty").assertExists()
        composeRule.onNodeWithTag("statistics_no_expenses").assertExists()
        composeRule.onNodeWithTag("statistics_donut").assertDoesNotExist()
        composeRule.onNodeWithTag("statistics_no_comparison").performScrollTo().assertIsDisplayed()
        composeRule.runOnIdle {
            state = StatisticsUiState(month, false, calculate(listOf(record(1, TransactionType.INCOME, 100, "Salary")), month))
        }
        composeRule.onNodeWithTag("statistics_no_comparison").assertDoesNotExist()
        composeRule.onNodeWithTag("statistics_no_expenses").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("statistics_empty").assertDoesNotExist()
        composeRule.runOnIdle { state = StatisticsUiState(month, false, error = StatisticsError.LOAD_FAILED) }
        composeRule.onNodeWithTag("statistics_income").assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.statistics_load_error)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertEquals(1, retries)
        composeRule.runOnIdle { state = state.copy(error = StatisticsError.CALCULATION_OVERFLOW) }
        composeRule.onNodeWithText(context.getString(R.string.statistics_overflow)).assertExists()
    }

    @Test
    fun populatedChartsLocalizePercentagesAndRemainReadableWithLargeTextAndAmounts() {
        var locale by mutableStateOf(Locale.forLanguageTag("vi"))
        var fontScale by mutableStateOf(1f)
        var large by mutableStateOf(false)
        val fixture = listOf(
            record(1, TransactionType.INCOME, 20_000_000, "Lương"),
            record(2, TransactionType.EXPENSE, 3_200_000, "Ăn uống"),
            record(3, TransactionType.EXPENSE, 2_500_000, "Mua sắm"),
            record(4, TransactionType.EXPENSE, 1_500_000, "Di chuyển"),
            record(5, TransactionType.EXPENSE, 1_000_000, "Hóa đơn"),
            record(6, TransactionType.EXPENSE, 6_000_000, "Past").copy(date = month.minusMonths(1).atDay(1)),
        )
        composeRule.setContent {
            val activity = LocalActivity.current as ComponentActivity
            LaunchedEffect(activity) { activity.enableEdgeToEdge() }
            val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
            val localized = context.createConfigurationContext(configuration)
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                val data = if (large) listOf(record(1, TransactionType.EXPENSE, Long.MAX_VALUE,
                    "A very long saved category that must wrap and remain readable")) else fixture
                PersonalExpenseManagerTheme {
                    StatisticsScreen(StatisticsUiState(month, false, calculate(data, month)), {}, {}, {})
                }
            }
        }
        for (tag in listOf("vi", "en")) {
            composeRule.runOnIdle { locale = Locale.forLanguageTag(tag) }
            composeRule.onNodeWithTag("statistics_content").performScrollToIndex(0)
            composeRule.onNodeWithTag("statistics_income").performScrollTo()
            composeRule.onNodeWithTag("statistics_donut").assertExists()
            val percent = NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 1 }.format(3_200_000.0 / 8_200_000)
            composeRule.onNodeWithText(percent, substring = true, useUnmergedTree = true).assertExists()
            composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(context.getExternalFilesDir(null), "statistics-$tag.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
            composeRule.onNodeWithTag("statistics_comparison").performScrollTo()
            composeRule.onNodeWithText(if (tag == "en") "Monthly income and expense" else "So sánh thu chi theo tháng").assertExists()
            composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(context.getExternalFilesDir(null), "statistics-comparison-$tag.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        composeRule.runOnIdle { fontScale = 1.5f; large = true }
        composeRule.onNodeWithTag("statistics_content").performScrollToIndex(0)
        composeRule.onNodeWithTag("statistics_expense").performScrollTo()
        val currency = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance("VND"); minimumFractionDigits = 0; maximumFractionDigits = 0
        }.format(Long.MAX_VALUE)
        composeRule.onNodeWithTag("statistics_expense").assertTextContains(currency)
        val text = composeRule.onNodeWithText("A very long saved category that must wrap and remain readable",
            useUnmergedTree = true).performScrollTo()
        text.assertIsDisplayed()
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        text.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow)
    }

    private fun record(id: Long, type: TransactionType, amount: Long, category: String) =
        Transaction(id, type, amount, category, month.atDay(1), null)
}
