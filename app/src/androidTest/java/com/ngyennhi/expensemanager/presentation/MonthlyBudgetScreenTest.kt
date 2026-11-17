package com.ngyennhi.expensemanager.presentation

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.MonthlyBudget
import com.ngyennhi.expensemanager.domain.model.MonthlySummary
import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.CategoryBudget
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.home.*
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import java.io.File
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonthlyBudgetScreenTest {
    @get:Rule val composeRule = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val month = YearMonth.of(2026, 10)
    private val actions = HomeActions({}, {}, {}, {}, {}, { _, _ -> })

    private fun ready(spent: Long, limit: Long?) = HomeUiState(selectedMonth = month,
        loadStatus = HomeLoadStatus.READY, budgetLoadStatus = HomeLoadStatus.READY,
        summary = MonthlySummary(month, 0, spent, -spent), budgetLimitVnd = limit,
        budget = limit?.let { MonthlyBudget(month, it, spent) }, budgetInput = limit?.toString().orEmpty())

    @Test fun sheetDistinguishesLoadingErrorUnsetZeroReachedAndExceeded() {
        var state by mutableStateOf(HomeUiState(selectedMonth = month))
        val locale = context.resources.configuration.locales[0]
        composeRule.setContent {
            PersonalExpenseManagerTheme {
                HomePanelSheet(HomePanel.BUDGET, state, locale, actions, {}, {}, {}, {})
            }
        }
        composeRule.onNodeWithTag("budget_overview").assertDoesNotExist()
        composeRule.onNodeWithTag("home_budget_input").assertDoesNotExist()
        composeRule.runOnIdle { state = state.copy(budgetLoadStatus = HomeLoadStatus.ERROR) }
        composeRule.onNodeWithText(context.getString(R.string.home_budget_load_error)).assertExists()
        composeRule.runOnIdle { state = ready(0, null) }
        composeRule.onNodeWithText(context.getString(R.string.home_budget_unset)).assertExists()
        composeRule.onNodeWithTag("budget_overview_progress").assertDoesNotExist()
        composeRule.runOnIdle { state = ready(0, 100) }
        composeRule.onNodeWithText(context.getString(R.string.budget_no_expenses)).assertExists()
        composeRule.onNodeWithTag("budget_overview_remaining").assertTextContains(homeVnd(100, locale))
        composeRule.onNodeWithTag("budget_warning").assertDoesNotExist()
        composeRule.runOnIdle { state = ready(100, 100) }
        composeRule.onNodeWithTag("budget_warning").assertTextEquals(context.getString(R.string.home_budget_reached))
        composeRule.runOnIdle { state = ready(150, 100) }
        composeRule.onNodeWithTag("budget_overview_remaining").assertTextContains(homeVnd(-50, locale))
        composeRule.onNodeWithTag("budget_warning").assertTextEquals(context.getString(R.string.home_budget_exceeded, homeVnd(50, locale)))
        val progress = composeRule.onNodeWithTag("budget_overview_progress").fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(1f, progress.current, 0f)
    }

    @Test fun overviewSupportsBothLocalesAndLargeAmountsAtLargerFontScale() {
        var locale by mutableStateOf(Locale.forLanguageTag("vi"))
        var textScale by mutableFloatStateOf(1f)
        var state by mutableStateOf(ready(8_200_000, 12_000_000))
        composeRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
            val localizedContext = context.createConfigurationContext(configuration)
            CompositionLocalProvider(LocalContext provides localizedContext,
                LocalResources provides localizedContext.resources,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(LocalDensity.current.density, textScale)) {
                PersonalExpenseManagerTheme {
                    HomePanelSheet(HomePanel.BUDGET, state, locale, actions, {}, {}, {}, {})
                }
            }
        }
        for (language in listOf("vi", "en")) {
            composeRule.runOnIdle { locale = Locale.forLanguageTag(language) }
            val localizedResources = context.createConfigurationContext(Configuration(context.resources.configuration)
                .apply { setLocale(locale) }).resources
            composeRule.onNodeWithTag("budget_overview_spent").assertTextContains(localizedResources.getString(R.string.budget_spent))
            composeRule.onNodeWithTag("home_budget_save").assertTextEquals(localizedResources.getString(R.string.home_save_budget))
            composeRule.onNodeWithTag("budget_overview_limit").assertTextEquals(homeVnd(12_000_000, locale))
            composeRule.onNodeWithTag("budget_overview_spent").assertTextContains(homeVnd(8_200_000, locale))
            composeRule.onNodeWithTag("budget_overview_remaining").assertTextContains(homeVnd(3_800_000, locale))
            composeRule.onNodeWithTag("home_panel_content").captureToImage().asAndroidBitmap().let { bitmap ->
                File(context.getExternalFilesDir(null), "task008-budget-$language.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        composeRule.runOnIdle { textScale = 1.5f; state = ready(Long.MAX_VALUE, 1) }
        composeRule.onNodeWithTag("budget_overview_spent").performScrollTo().assertTextContains(homeVnd(Long.MAX_VALUE, locale))
        composeRule.onNodeWithTag("budget_overview_remaining").performScrollTo().assertTextContains(homeVnd(1 - Long.MAX_VALUE, locale))
        composeRule.onNodeWithTag("home_budget_save").performScrollTo().assertIsDisplayed()
    }

    @Test fun categoryCardsShowExactProgressWarningsAndArchivedRemoval() {
        val active = Category(1, TransactionType.EXPENSE, "Food", false)
        val archived = Category(2, TransactionType.EXPENSE, "Old", true)
        var edited: Long? = null
        var removed: Long? = null
        val categoryActions = actions.copy(
            onEditCategoryBudget = { edited = it },
            onRemoveCategoryBudget = { removed = it },
        )
        val state = ready(0, null).copy(
            categoryBudgetLoadStatus = HomeLoadStatus.READY,
            categoryBudgetItems = listOf(
                CategoryBudgetItem(active, CategoryBudget(active.id, month, 100, 150)),
                CategoryBudgetItem(archived, CategoryBudget(archived.id, month, 80, 20)),
            ),
            activeExpenseCategories = listOf(active),
        )
        val locale = context.resources.configuration.locales[0]
        composeRule.setContent {
            PersonalExpenseManagerTheme {
                HomePanelSheet(HomePanel.BUDGET, state, locale, categoryActions, {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithTag("category_budget_1").performScrollTo().assertExists()
        composeRule.onNodeWithText("Food").assertExists()
        composeRule.onNodeWithText(context.getString(
            R.string.category_budget_spent_limit,
            homeVnd(150, locale),
            homeVnd(100, locale),
        )).assertExists()
        composeRule.onNodeWithTag("category_budget_warning_1")
            .assertTextEquals(context.getString(R.string.category_budget_exceeded, homeVnd(50, locale)))
        val progress = composeRule.onNodeWithTag("category_budget_progress_1").fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(1f, progress.current, 0f)
        composeRule.onNodeWithTag("category_budget_archived_2").performScrollTo().assertExists()
        composeRule.onNodeWithTag("category_budget_edit_2").assertDoesNotExist()
        composeRule.onNodeWithTag("category_budget_edit_1").performClick()
        composeRule.onNodeWithTag("category_budget_remove_2").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(1L, edited); assertEquals(2L, removed) }
    }

    @Test fun categoryEditorShowsOnlyAvailableActiveChoicesAndKeepsAccessibleActions() {
        val food = Category(1, TransactionType.EXPENSE, "Food", false)
        val travel = Category(2, TransactionType.EXPENSE, "Travel", false)
        var selected: Long? = null
        var input = ""
        var saved = false
        val categoryActions = actions.copy(
            onSelectCategoryBudgetCategory = { selected = it },
            onCategoryBudgetInput = { input = it },
            onSaveCategoryBudget = { saved = true },
        )
        val state = ready(0, null).copy(
            categoryBudgetLoadStatus = HomeLoadStatus.READY,
            categoryBudgetItems = listOf(CategoryBudgetItem(food, CategoryBudget(food.id, month, 100, 0))),
            activeExpenseCategories = listOf(food, travel),
            categoryBudgetEditor = CategoryBudgetEditorState(categoryId = travel.id),
        )
        composeRule.setContent {
            PersonalExpenseManagerTheme {
                HomePanelSheet(HomePanel.BUDGET, state, Locale.ENGLISH, categoryActions, {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithTag("category_budget_choice_1").assertDoesNotExist()
        composeRule.onNodeWithTag("category_budget_choice_2").performScrollTo().assertTextContains("Travel").performClick()
        composeRule.onNodeWithTag("category_budget_input").performTextInput("250")
        composeRule.onNodeWithTag("category_budget_save").performScrollTo().assertHasClickAction().performClick()
        composeRule.runOnIdle {
            assertEquals(2L, selected)
            assertEquals("250", input)
            assertTrue(saved)
        }
    }
}
