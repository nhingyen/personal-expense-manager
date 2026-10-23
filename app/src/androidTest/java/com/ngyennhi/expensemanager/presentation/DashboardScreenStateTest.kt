package com.ngyennhi.expensemanager.presentation

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.domain.model.MonthlyBudget
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardError
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardLoadStatus
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardScreen
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardUiState
import com.ngyennhi.expensemanager.presentation.dashboard.DashboardHomeActions
import com.ngyennhi.expensemanager.presentation.common.components.PrimaryNavigationBar
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import java.text.NumberFormat
import java.io.File
import java.time.YearMonth
import java.util.Currency
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenStateTest {
    @get:Rule val composeRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val month = YearMonth.of(2026, 10)
    private val homeActions = DashboardHomeActions({}, {}, {}, {}, {}, { _, _ -> })
    private val record = Transaction(42, TransactionType.EXPENSE, 150_000,
        "User category", month.atDay(19), "User note")

    @Test
    fun loadingDoesNotDisplayZerosOrEmptySuccessAndMonthControlsWork() {
        var previous = 0
        var next = 0
        render(DashboardUiState(selectedMonth = month),
            onPrevious = { previous++ }, onNext = { next++ })
        composeRule.onNodeWithTag("dashboard_loading").assertExists()
        composeRule.onNodeWithTag("dashboard_summary").assertDoesNotExist()
        composeRule.onNodeWithTag("dashboard_empty").assertDoesNotExist()
        composeRule.onNodeWithTag("home_month_picker").performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.previous_month)).performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.next_month)).performClick()
        composeRule.onNodeWithTag("home_month_done").performClick()
        assertEquals(1, previous)
        assertEquals(1, next)
    }

    @Test
    fun observedEmptyShowsZeroTotalsAndAddAndViewAllCallbacks() {
        var added = 0
        var viewed = 0
        render(ready(MonthlySummary(month, 0, 0, 0)), onAdd = { added++ }, onViewAll = { viewed++ })
        composeRule.onNodeWithTag("dashboard_net", useUnmergedTree = true).assertTextEquals(currency(0))
        composeRule.onNodeWithTag("dashboard_income", useUnmergedTree = true).assertTextEquals(currency(0))
        composeRule.onNodeWithTag("dashboard_expense", useUnmergedTree = true).assertTextEquals(currency(0))
        composeRule.onNodeWithTag("dashboard_view_all").performScrollTo().performClick()
        composeRule.onNodeWithTag("dashboard_add").performScrollTo().performClick()
        assertEquals(1, viewed)
        assertEquals(1, added)
    }

    @Test
    fun negativeNetAndActualRecordAreRenderedAndExactIdIsForwarded() {
        var opened: Long? = null
        render(ready(MonthlySummary(month, 100_000, 150_000, -50_000)).copy(
            recentTransactions = listOf(record)), onOpen = { opened = it })
        composeRule.onNodeWithTag("dashboard_net", useUnmergedTree = true).assertTextEquals(currency(-50_000))
        composeRule.onNodeWithTag("dashboard_empty").assertDoesNotExist()
        composeRule.onNodeWithTag("transaction_row_42").performScrollTo().performClick()
        assertEquals(42L, opened)
        composeRule.onNodeWithText(record.category, substring = true).assertExists()
    }

    @Test
    fun initialErrorAndRetryNeverShowEmptySuccessOrFabricatedTotals() {
        var state by mutableStateOf(DashboardUiState(selectedMonth = month,
            loadStatus = DashboardLoadStatus.ERROR, error = DashboardError.DATA_LOAD_FAILED))
        var retries = 0
        composeRule.setContent {
            PersonalExpenseManagerTheme {
                DashboardScreen(state, {}, {}, {}, {}, {}, { retries++ }, homeActions)
            }
        }
        composeRule.onNodeWithTag("dashboard_summary").assertDoesNotExist()
        composeRule.onNodeWithTag("dashboard_empty").assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertEquals(1, retries)
        composeRule.runOnIdle { state = state.copy(isRetrying = true) }
        composeRule.onNodeWithTag("dashboard_loading").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.retry)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.dashboard_load_error)).assertExists()
    }

    @Test
    fun staleSnapshotIsIdentifiedAndOverflowHidesFinancialData() {
        var state by mutableStateOf(ready(MonthlySummary(month, 100_000, 150_000, -50_000)).copy(
            loadStatus = DashboardLoadStatus.ERROR, error = DashboardError.DATA_LOAD_FAILED,
            recentTransactions = listOf(record), isStale = true))
        composeRule.setContent {
            PersonalExpenseManagerTheme { DashboardScreen(state, {}, {}, {}, {}, {}, {}, homeActions) }
        }
        composeRule.onNodeWithTag("dashboard_stale").assertExists()
        composeRule.onNodeWithTag("dashboard_summary").assertExists()
        composeRule.runOnIdle { state = state.copy(error = DashboardError.CALCULATION_OVERFLOW) }
        composeRule.onNodeWithText(context.getString(R.string.dashboard_calculation_error)).assertExists()
        composeRule.onNodeWithTag("dashboard_summary").assertDoesNotExist()
        composeRule.onNodeWithTag("transaction_row_42").assertDoesNotExist()
        composeRule.onNodeWithTag("dashboard_empty").assertDoesNotExist()
    }

    @Test
    fun summaryForAnotherMonthIsNeverShownUnderSelectedMonth() {
        render(ready(MonthlySummary(month.minusMonths(1), 100_000, 0, 100_000)))
        composeRule.onNodeWithTag("dashboard_summary").assertDoesNotExist()
        composeRule.onNodeWithTag("dashboard_empty").assertDoesNotExist()
    }

    @Test
    fun vietnameseAndEnglishSupportLargeAmountsAndLargerText() {
        var language by mutableStateOf("vi")
        val total = 20_000_000_000L
        val state = ready(MonthlySummary(month, total, 150_000, total - 150_000)).copy(
            recentTransactions = listOf(record.copy(category = "Danh mục dài / A long user category")))
        composeRule.setContent {
            UseAppWindowInsets()
            val configuration = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
                fontScale = 1.3f
            }
            val localizedContext = context.createConfigurationContext(configuration)
            val density = LocalDensity.current.density
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(density, configuration.fontScale),
            ) {
                PersonalExpenseManagerTheme { DashboardScreen(state, {}, {}, {}, {}, {}, {}, homeActions) }
            }
        }
        for (locale in listOf("vi", "en")) {
            composeRule.runOnIdle { language = locale }
            val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(locale))
            }).resources
            composeRule.onNodeWithText(resources.getString(R.string.dashboard_title)).assertExists()
            composeRule.onNodeWithText(resources.getString(R.string.dashboard_net_income)).assertExists()
            val amount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(locale)).apply {
                currency = Currency.getInstance("VND")
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }.format(total - 150_000)
            composeRule.onNodeWithTag("dashboard_net", useUnmergedTree = true).assertTextEquals(amount)
            composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(context.getExternalFilesDir(null), "dashboard-$locale.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
            composeRule.onNodeWithTag("transaction_row_42").performScrollTo().performClick()
        }
    }

    @Test
    fun localizedIntegratedLayoutHasAccessibleControlsAndDistinctEmptyErrorRetryStates() {
        var language by mutableStateOf("vi")
        var state by mutableStateOf(ready(MonthlySummary(month, 0, 0, 0)))
        composeRule.setContent {
            UseAppWindowInsets()
            val configuration = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
                fontScale = 1.3f
            }
            val localized = context.createConfigurationContext(configuration)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, configuration.fontScale),
            ) {
                PersonalExpenseManagerTheme {
                    DashboardScreen(state, {}, {}, {}, {}, {}, {}, homeActions,
                        bottomBar = { PrimaryNavigationBar(true, {}, {}, {}) }, showFloatingAdd = false)
                }
            }
        }
        for (languageTag in listOf("vi", "en")) {
            composeRule.runOnIdle {
                language = languageTag
                state = ready(MonthlySummary(month, 0, 0, 0))
            }
            val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(languageTag))
            }).resources
            composeRule.onNodeWithTag("nav_home").assertIsSelected().assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            composeRule.onNodeWithTag("nav_add").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            composeRule.onNodeWithTag("home_month_picker").performClick()
            composeRule.onNodeWithTag("month_previous").assertHeightIsAtLeast(48.dp)
            composeRule.onNodeWithTag("month_next").assertHeightIsAtLeast(48.dp)
            composeRule.onNodeWithTag("home_month_done").performClick()
            composeRule.onNodeWithTag("dashboard_empty").performScrollTo().assertExists()
            composeRule.runOnIdle { state = DashboardUiState(selectedMonth = month,
                loadStatus = DashboardLoadStatus.ERROR, error = DashboardError.DATA_LOAD_FAILED) }
            composeRule.onNodeWithTag("dashboard_empty").assertDoesNotExist()
            composeRule.onNodeWithText(resources.getString(R.string.dashboard_load_error)).performScrollTo().assertExists()
            composeRule.onNodeWithText(resources.getString(R.string.retry)).assertExists()
            composeRule.runOnIdle { state = state.copy(isRetrying = true) }
            composeRule.onNodeWithTag("dashboard_loading").performScrollTo().assertExists()
            composeRule.onNodeWithText(resources.getString(R.string.retry)).assertDoesNotExist()
            composeRule.runOnIdle { state = ready(MonthlySummary(month, 0, Long.MAX_VALUE, -Long.MAX_VALUE)).copy(
                recentTransactions = listOf(record.copy(amountVnd = Long.MAX_VALUE,
                    category = "Danh mục rất dài / A very long category", note = "Ghi chú dài / A long note"))) }
            val negativeAmount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag(languageTag)).apply {
                currency = Currency.getInstance("VND")
                minimumFractionDigits = 0
                maximumFractionDigits = 0
            }.format(-Long.MAX_VALUE)
            composeRule.onNodeWithTag("dashboard_net", useUnmergedTree = true).assertTextEquals(negativeAmount)
            composeRule.onNodeWithTag("dashboard_summary").performScrollTo().assertExists()
            composeRule.onNodeWithTag("transaction_row_42").performScrollTo().assertExists()
            composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(context.getExternalFilesDir(null), "dashboard-integrated-$languageTag.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    @Test
    fun figmaReferenceUsesCompactTwoColumnTotalsAndAdaptsWithoutClippingExactAmounts() {
        var textScale by mutableStateOf(1f)
        var largeValues by mutableStateOf(false)
        val locale = Locale.forLanguageTag("vi")
        composeRule.setContent {
            UseAppWindowInsets()
            val configuration = Configuration(context.resources.configuration).apply { setLocale(locale); fontScale = textScale }
            val localized = context.createConfigurationContext(configuration)
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, textScale),
            ) {
                val summary = if (largeValues) MonthlySummary(month, 0, Long.MAX_VALUE, -Long.MAX_VALUE)
                    else MonthlySummary(month, 20_000_000, 8_200_000, 11_800_000)
                val records = listOf(
                    record.copy(category = "Ăn uống", amountVnd = 85_000, note = "Bữa trưa"),
                    record.copy(id = 43, category = "Cà phê", amountVnd = 45_000, note = "Cà phê sáng"),
                )
                PersonalExpenseManagerTheme {
                    DashboardScreen(ready(summary).copy(recentTransactions = records,
                        budgetLimitVnd = 12_000_000,
                        budget = MonthlyBudget(month, 12_000_000, summary.expenseVnd)), {}, {}, {}, {}, {}, {}, homeActions,
                        bottomBar = { PrimaryNavigationBar(true, {}, {}, {}) }, showFloatingAdd = false)
                }
            }
        }
        composeRule.onNodeWithTag("app_top_bar").assertHeightIsEqualTo(64.dp)
        composeRule.onNodeWithTag("primary_navigation").assertHeightIsEqualTo(80.dp)
        composeRule.onNodeWithTag("dashboard_summary").assertHeightIsEqualTo(174.dp)
        composeRule.onNodeWithTag("home_budget_card").assertHeightIsEqualTo(92.dp)
        composeRule.onNodeWithTag("home_budget_percentage", useUnmergedTree = true).assertTextEquals("Đã dùng 68%")
        composeRule.onNodeWithTag("home_greeting").assertExists()
        composeRule.onNodeWithTag("home_notifications_button").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("home_budget_shortcut").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("home_categories_shortcut").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Thu nhập tháng 10").assertExists()
        val income = composeRule.onNodeWithTag("dashboard_income", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val expense = composeRule.onNodeWithTag("dashboard_expense", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(income.top, expense.top, 1f)
        assertTrue(expense.left > income.right)
        composeRule.onNodeWithTag("transaction_row_42").assertHeightIsEqualTo(64.dp)
        composeRule.onNodeWithText("Bữa trưa").assertExists()
        composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            File(context.getExternalFilesDir(null), "dashboard-figma-vi.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        composeRule.runOnIdle { textScale = 1.3f; largeValues = true }
        composeRule.onNodeWithTag("dashboard_summary").performScrollTo()
        val largeIncome = composeRule.onNodeWithTag("dashboard_income", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val largeExpense = composeRule.onNodeWithTag("dashboard_expense", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(largeExpense.top > largeIncome.bottom)
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance("VND"); maximumFractionDigits = 0; minimumFractionDigits = 0
        }
        composeRule.onNodeWithTag("dashboard_net", useUnmergedTree = true).assertTextEquals(formatter.format(-Long.MAX_VALUE))
        composeRule.onNodeWithTag("dashboard_expense", useUnmergedTree = true).assertTextEquals(formatter.format(Long.MAX_VALUE))
    }

    @Composable
    private fun UseAppWindowInsets() {
        val activity = LocalActivity.current as ComponentActivity
        LaunchedEffect(activity) { activity.enableEdgeToEdge() }
    }

    private fun ready(summary: MonthlySummary) = DashboardUiState(
        selectedMonth = month, loadStatus = DashboardLoadStatus.READY, summary = summary,
        greetingDateTime = month.atDay(19).atTime(9, 41), budgetLoadStatus = DashboardLoadStatus.READY)

    private fun render(
        state: DashboardUiState,
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
        onAdd: () -> Unit = {},
        onViewAll: () -> Unit = {},
        onOpen: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            PersonalExpenseManagerTheme {
                DashboardScreen(state, onPrevious, onNext, onAdd, onViewAll, onOpen, {}, homeActions)
            }
        }
    }

    private fun currency(amount: Long): String = NumberFormat.getCurrencyInstance(context.resources.configuration.locales[0])
        .apply {
            currency = Currency.getInstance("VND")
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }.format(amount)
}
