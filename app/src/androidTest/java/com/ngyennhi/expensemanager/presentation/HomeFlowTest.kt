package com.ngyennhi.expensemanager.presentation

import android.content.Context
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngyennhi.expensemanager.MainActivity
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.local.TransactionDao
import com.ngyennhi.expensemanager.di.TransactionDatabaseModule
import com.ngyennhi.expensemanager.di.MonthlyBudgetStorageModule
import com.ngyennhi.expensemanager.domain.repository.MonthlyBudgetRepository
import com.ngyennhi.expensemanager.domain.model.TransactionDraft
import com.ngyennhi.expensemanager.domain.model.TransactionDateRange
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.presentation.home.HomeLoadStatus
import com.ngyennhi.expensemanager.presentation.home.HomeViewModel
import com.ngyennhi.expensemanager.presentation.statistics.StatisticsViewModel
import com.ngyennhi.expensemanager.presentation.transaction.TransactionFilter
import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(TransactionDatabaseModule::class, MonthlyBudgetStorageModule::class)
@RunWith(AndroidJUnit4::class)
class HomeFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var database: ExpenseManagerDatabase
    @Inject lateinit var repository: TransactionRepository
    private val home get() = ViewModelProvider(composeRule.activity)[HomeViewModel::class.java]
    private val transactions get() = ViewModelProvider(composeRule.activity)[TransactionViewModel::class.java]

    @Before fun setUp() { hiltRule.inject(); awaitReady() }
    @After fun tearDown() { database.close() }

    @Test
    fun statisticsObservesRoomChangesAndPreservesItsMonthAcrossTabsAndRecreation() {
        val statistics = ViewModelProvider(composeRule.activity)[StatisticsViewModel::class.java]
        val selectedMonth = statistics.uiState.value.selectedMonth.minusMonths(1)
        composeRule.onNodeWithTag("nav_statistics").performClick()
        composeRule.onNodeWithTag("month_previous").performClick()
        composeRule.waitUntil(5_000) { statistics.uiState.value.statistics?.summary?.month == selectedMonth }
        val id = runBlocking {
            repository.add(TransactionDraft(TransactionType.EXPENSE, 125, "Saved category", selectedMonth.atDay(1), null))
        }
        composeRule.waitUntil(5_000) { statistics.uiState.value.statistics?.summary?.expenseVnd == 125L }
        composeRule.onNodeWithText("Saved category").assertExists()
        val saved = runBlocking { repository.observeTransactions().first().single() }
        runBlocking { repository.update(saved.copy(amountVnd = 250, type = TransactionType.INCOME)) }
        composeRule.waitUntil(5_000) { statistics.uiState.value.statistics?.summary?.incomeVnd == 250L }
        composeRule.onNodeWithTag("statistics_no_expenses").assertExists()
        composeRule.onNodeWithTag("nav_home").performClick()
        composeRule.onNodeWithTag("nav_statistics").performClick()
        assertEquals(selectedMonth, statistics.uiState.value.selectedMonth)
        composeRule.activityRule.scenario.recreate()
        awaitReady()
        composeRule.onNodeWithTag("nav_statistics").assertIsSelected()
        val recreated = ViewModelProvider(composeRule.activity)[StatisticsViewModel::class.java]
        assertEquals(selectedMonth, recreated.uiState.value.selectedMonth)
        runBlocking { repository.delete(id) }
        composeRule.waitUntil(5_000) { recreated.uiState.value.statistics?.summary?.incomeVnd == 0L }
        composeRule.onNodeWithTag("statistics_empty").assertExists()
        Espresso.pressBack()
        composeRule.onNodeWithTag("nav_home").assertIsSelected()
    }

    @Test
    fun primaryTabsPreserveFiltersAndRecreationRestoresSelectedDestination() {
        val month = home.uiState.value.selectedMonth.minusMonths(2)
        composeRule.onNodeWithTag("nav_home").assertIsSelected()
        composeRule.onNodeWithTag("nav_transactions").performClick().assertIsSelected()
        composeRule.onNodeWithTag("transaction_search").performTextInput("Coffee")
        Espresso.closeSoftKeyboard()
        composeRule.runOnIdle {
            transactions.selectMonth(month)
            transactions.setFilter(TransactionFilter.EXPENSE)
            transactions.applyFilters("Coffee", TransactionDateRange(month.atDay(2), month.plusMonths(1).atDay(4)))
            home.changeMonth(-1)
        }
        val homeMonth = home.uiState.value.selectedMonth
        repeat(3) {
            composeRule.onNodeWithTag("nav_statistics").performClick().assertIsSelected()
            composeRule.onNodeWithTag("statistics_screen").assertExists()
            composeRule.onNodeWithTag("nav_settings").performClick().assertIsSelected()
            composeRule.onNodeWithTag("settings_placeholder").assertExists()
            composeRule.onNodeWithTag("nav_home").assertIsNotSelected().performClick().assertIsSelected()
            composeRule.onNodeWithTag("nav_transactions").performClick().performClick()
            composeRule.onNodeWithTag("transaction_search").assertTextEquals("Coffee")
            assertEquals(month, transactions.uiState.value.selectedMonth)
            assertEquals(TransactionFilter.EXPENSE, transactions.uiState.value.filter)
            assertEquals("Coffee", transactions.uiState.value.selectedCategory)
            assertEquals(month.plusMonths(1).atDay(4), transactions.uiState.value.dateRange?.endInclusive)
            assertEquals(homeMonth, home.uiState.value.selectedMonth)
        }
        composeRule.activityRule.scenario.recreate()
        awaitReady()
        composeRule.onNodeWithTag("nav_transactions").assertIsSelected()
        composeRule.onNodeWithTag("transaction_search").assertTextEquals("Coffee")
        assertEquals("Coffee", transactions.uiState.value.selectedCategory)
        assertEquals(month.atDay(2), transactions.uiState.value.dateRange?.start)
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.activityRule.scenario.recreate()
        awaitReady()
        composeRule.onNodeWithTag("nav_settings").assertIsSelected()
        Espresso.pressBack()
        composeRule.onNodeWithTag("nav_home").assertIsSelected()
    }

    @Test
    fun combinedSearchCategoryAndRangeReactToRoomWritesAndResetFromNoMatches() {
        val month = home.uiState.value.selectedMonth
        val first = month.minusMonths(1).atEndOfMonth()
        val last = month.atDay(2)
        val ids = runBlocking {
            listOf(
                repository.add(TransactionDraft(TransactionType.EXPENSE, 100, "Food", first, "Lunch")),
                repository.add(TransactionDraft(TransactionType.EXPENSE, 100, "Food", last, "Lunch")),
                repository.add(TransactionDraft(TransactionType.INCOME, 100, "Salary", last, "Lunch")),
                repository.add(TransactionDraft(TransactionType.EXPENSE, 100, "Travel", last, "Lunch")),
            )
        }
        composeRule.waitUntil(5_000) { transactions.uiState.value.transactions.size == 4 }
        composeRule.onNodeWithTag("nav_transactions").performClick()
        composeRule.onNodeWithTag("transaction_search").performTextInput("LUNCH")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.type_expense)).performClick()
        composeRule.onNodeWithTag("open_transaction_filters").performClick()
        composeRule.onNodeWithTag("filter_category").performClick()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithTag("cancel_transaction_filters").performScrollTo().performClick()
        assertNull(transactions.uiState.value.selectedCategory)
        composeRule.onNodeWithTag("open_transaction_filters").performClick()
        composeRule.onNodeWithTag("filter_category").performClick()
        composeRule.onNodeWithText("Food").performClick()
        composeRule.onNodeWithTag("apply_transaction_filters").performScrollTo().performClick()
        assertEquals("Food", transactions.uiState.value.selectedCategory)
        composeRule.runOnIdle { transactions.applyFilters("Food", TransactionDateRange(first, last)) }
        composeRule.onNodeWithTag("transaction_row_${ids[0]}").assertExists()
        composeRule.onNodeWithTag("transaction_row_${ids[1]}").assertExists()
        composeRule.onNodeWithTag("transaction_row_${ids[2]}").assertDoesNotExist()
        composeRule.onNodeWithTag("transaction_row_${ids[3]}").assertDoesNotExist()
        val added = runBlocking {
            repository.add(TransactionDraft(TransactionType.EXPENSE, 100, "Food", last, "Lunch"))
        }
        composeRule.waitUntil(5_000) { transactions.uiState.value.visibleTransactions.size == 3 }
        runBlocking {
            val record = repository.observeTransactions().first().first { it.id == added }
            repository.update(record.copy(category = "Travel"))
            repository.delete(ids[0])
            repository.delete(ids[1])
        }
        composeRule.waitUntil(5_000) { transactions.uiState.value.visibleTransactions.isEmpty() }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.no_matches_title)).assertIsDisplayed()
        composeRule.onNodeWithTag("active_filter_category").assertExists()
        composeRule.onNodeWithTag("empty_reset_filters").performClick()
        assertFalse(transactions.uiState.value.hasActiveFilters)
        assertEquals(YearMonth.now(), transactions.uiState.value.selectedMonth)
        composeRule.onNodeWithTag("transaction_row_$added").assertExists()
        assertEquals(3, runBlocking { repository.observeTransactions().first().size })
    }

    @Test
    fun addActionFromEveryPrimaryTabHidesBarAndBackReturnsToInvoker() {
        for (tab in listOf("home", "transactions", "statistics", "settings")) {
            composeRule.onNodeWithTag("nav_$tab").performClick().performClick()
            composeRule.onNodeWithTag("nav_add")
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected)).performClick()
            composeRule.onNodeWithTag("app_bottom_bar").assertDoesNotExist()
            composeRule.onNodeWithTag("amount_input").performTextInput("123")
            Espresso.closeSoftKeyboard()
            composeRule.activityRule.scenario.recreate()
            awaitReady()
            composeRule.onNodeWithTag("app_bottom_bar").assertDoesNotExist()
            composeRule.onNodeWithTag("amount_input").assertTextContains("123", substring = true)
            assertEquals("123", transactions.uiState.value.form.amountInput)
            composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
            composeRule.onNodeWithTag("nav_$tab").assertIsSelected()
        }
        assertTrue(runBlocking { repository.observeTransactions().first().isEmpty() })
        Espresso.pressBack()
        composeRule.onNodeWithTag("nav_home").assertIsSelected()
    }

    @Test
    fun listScrollRestoresAcrossTabSwitchAndRecreation() {
        val date = home.uiState.value.selectedMonth.atDay(15)
        runBlocking {
            repeat(30) { index ->
                repository.add(TransactionDraft(TransactionType.EXPENSE, 100, "Food", date, "Record $index"))
            }
        }
        composeRule.waitUntil(5_000) { transactions.uiState.value.transactions.size == 30 }
        composeRule.onNodeWithTag("nav_transactions").performClick()
        val id = transactions.uiState.value.visibleTransactions[20].id
        composeRule.onNodeWithTag("transaction_list").performScrollToIndex(20)
        composeRule.onNodeWithTag("transaction_row_$id").assertIsDisplayed()
        val before = composeRule.onNodeWithTag("transaction_row_$id").fetchSemanticsNode().boundsInRoot.top
        composeRule.onNodeWithTag("nav_statistics").performClick()
        composeRule.onNodeWithTag("nav_transactions").performClick()
        composeRule.onNodeWithTag("transaction_row_$id").assertIsDisplayed()
        assertEquals(before, composeRule.onNodeWithTag("transaction_row_$id").fetchSemanticsNode().boundsInRoot.top, 1f)
        composeRule.activityRule.scenario.recreate()
        awaitReady()
        composeRule.onNodeWithTag("nav_transactions").assertIsSelected()
        composeRule.onNodeWithTag("transaction_row_$id").assertIsDisplayed()
    }

    @Test
    fun directDetailEditHideBarAndSuccessfulSaveSelectsTransactionsWithoutSecondaryHistory() {
        val id = seed()
        openRecent(id)
        composeRule.onNodeWithTag("app_bottom_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("edit_transaction_button").performClick()
        composeRule.onNodeWithTag("app_bottom_bar").assertDoesNotExist()
        composeRule.runOnIdle { transactions.setAmount("200") }
        composeRule.onNodeWithTag("save_transaction_button").performClick()
        awaitRows(1)
        composeRule.onNodeWithTag("nav_transactions").assertIsSelected()
        Espresso.pressBack()
        composeRule.onNodeWithTag("nav_home").assertIsSelected()
    }

    @Test
    fun budgetSetupValidationRoomUpdatesNotificationsAndClearingWork() {
        composeRule.onNodeWithTag("home_greeting").assertExists()
        composeRule.onNodeWithTag("home_notifications_button").performClick()
        composeRule.onNodeWithTag("home_no_notifications").assertExists()
        Espresso.pressBack()
        composeRule.onNodeWithTag("home_budget_shortcut").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_budget_input").performTextInput("0")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("home_budget_save").performScrollTo().performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.amount_invalid)).assertExists()
        composeRule.onNodeWithTag("home_budget_input").performTextClearance()
        composeRule.onNodeWithTag("home_budget_input").performTextInput("12000000")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("home_budget_save").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { home.uiState.value.budgetLimitVnd == 12_000_000L && !home.uiState.value.isSavingBudget }
        assertEquals(0L, home.uiState.value.budget?.spentVnd)
        val id = seed()
        assertEquals(100L, home.uiState.value.budget?.spentVnd)
        runBlocking {
            val record = repository.observeTransactions().first().single()
            repository.update(record.copy(amountVnd = 15_000_000))
        }
        composeRule.waitUntil(5_000) { home.uiState.value.budget?.remainingVnd == -3_000_000L }
        composeRule.onNodeWithTag("home_notifications_button").performClick()
        composeRule.onNodeWithTag("home_budget_notice").assertExists()
        Espresso.pressBack()
        composeRule.onNodeWithTag("home_budget_card").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_budget_clear").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { home.uiState.value.budgetLimitVnd == null && !home.uiState.value.isSavingBudget }
        composeRule.onNodeWithTag("home_budget_progress").assertDoesNotExist()
        assertNull(home.uiState.value.budget)
        assertEquals(id, runBlocking { repository.observeTransactions().first().single().id })
    }

    @Test
    fun budgetSheetSelectsMonthEditsAndCancelsWithoutMixingLimits() {
        val initialMonth = home.uiState.value.selectedMonth
        val id = seed()
        composeRule.onNodeWithTag("home_budget_shortcut").performScrollTo().performClick()
        composeRule.onNodeWithTag("budget_overview_progress").assertDoesNotExist()
        composeRule.onNodeWithTag("home_budget_input").performTextInput("100")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("home_budget_save").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { home.uiState.value.budget?.limitReached == true && !home.uiState.value.isSavingBudget }
        composeRule.onNodeWithTag("home_budget_card").performScrollTo().performClick()
        composeRule.onNodeWithTag("budget_warning").assertTextEquals(composeRule.activity.getString(R.string.home_budget_reached))
        composeRule.onNodeWithTag("home_budget_input").performTextClearance()
        composeRule.onNodeWithTag("home_budget_input").performTextInput("999")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("month_next").performScrollTo().performClick()
        assertEquals(initialMonth.plusMonths(1), home.uiState.value.selectedMonth)
        composeRule.onNodeWithTag("home_budget_input").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.budget_no_expenses)).assertExists()
        composeRule.onNodeWithTag("home_budget_input").performTextInput("300")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("home_budget_save").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { home.uiState.value.budget?.remainingVnd == 300L && !home.uiState.value.isSavingBudget }
        composeRule.onNodeWithTag("home_budget_card").performScrollTo().performClick()
        composeRule.onNodeWithTag("month_previous").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_budget_input").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("100")))
        composeRule.onNodeWithTag("home_budget_input").performTextClearance()
        composeRule.onNodeWithTag("home_budget_input").performTextInput("200")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("home_budget_save").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { home.uiState.value.budget?.remainingVnd == 100L && !home.uiState.value.isSavingBudget }
        composeRule.onNodeWithTag("home_budget_card").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_budget_input").performTextClearance()
        composeRule.onNodeWithTag("home_budget_input").performTextInput("500")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.cancel)).performScrollTo().performClick()
        composeRule.onNodeWithTag("home_budget_card").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_budget_input").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("200")))
        Espresso.pressBack()
        composeRule.activityRule.scenario.recreate(); awaitReady()
        assertEquals(initialMonth, home.uiState.value.selectedMonth)
        assertEquals(200L, home.uiState.value.budgetLimitVnd)
        assertEquals(id, runBlocking { repository.observeTransactions().first().single().id })
    }

    @Test
    fun categoryShortcutReusesSuggestionsAndOpensPrefilledAddWithoutWritingOnCancel() {
        composeRule.onNodeWithTag("home_categories_shortcut").performScrollTo().performClick()
        val category = composeRule.activity.getString(R.string.category_coffee)
        composeRule.onNodeWithText(category).performScrollTo().performClick()
        composeRule.onNodeWithTag("category_input").assertExists()
        assertEquals(category, transactions.uiState.value.form.category)
        assertEquals(TransactionType.EXPENSE, transactions.uiState.value.form.type)
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.onNodeWithTag("home_greeting").assertExists()
        assertTrue(runBlocking { repository.observeTransactions().first().isEmpty() })
    }

    @Test
    fun launchesHomeAndDirectAddSaveCreatesListAndUpdatesHome() {
        composeRule.onNodeWithTag("home_greeting").assertExists()
        assertTrue(home.uiState.value.isEmpty)
        composeRule.onNodeWithTag("home_add").performClick()
        composeRule.onNodeWithTag("amount_input").performTextInput("85000")
        composeRule.onNodeWithTag("category_input").performTextInput("Food")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("save_transaction_button").performClick()
        awaitRows(1)
        composeRule.onNodeWithTag("transactions_screen").assertExists()
        Espresso.pressBack()
        awaitNet(-85_000)
        val id = home.uiState.value.recentTransactions.single().id
        composeRule.onNodeWithTag("transaction_row_$id").performScrollTo().performClick()
        Espresso.pressBack()
        composeRule.onNodeWithTag("home_greeting").assertExists()
        assertEquals(-85_000L, home.uiState.value.summary?.netVnd)
    }

    @Test
    fun directHomeEditUpdatesAmountTypeDateAndSelectsSavedMonthInTransactions() {
        val month = home.uiState.value.selectedMonth
        val id = seed(month.atDay(15))
        openRecent(id)
        composeRule.onNodeWithTag("edit_transaction_button").performClick()
        // Use the existing event handlers for type/date; TASK-003 covers form widgets.
        composeRule.runOnIdle {
            transactions.setAmount("250000")
            transactions.setType(TransactionType.INCOME)
            transactions.setDate(month.plusMonths(1).atDay(1))
        }
        composeRule.onNodeWithTag("save_transaction_button").performClick()
        composeRule.waitUntil(5_000) { home.uiState.value.isEmpty }
        composeRule.onNodeWithTag("transactions_screen").assertExists()
        assertEquals(month.plusMonths(1), transactions.uiState.value.selectedMonth)
        assertEquals(id, runBlocking { repository.observeTransactions().first().single().id })
        Espresso.pressBack()
        composeRule.onNodeWithTag("home_empty").performScrollTo().assertExists()
        composeRule.onNodeWithTag("home_month_picker").performClick()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.next_month)).performClick()
        composeRule.onNodeWithTag("home_month_done").performClick()
        awaitNet(250_000)
        assertEquals(listOf(id), home.uiState.value.recentTransactions.map { it.id })
    }

    @Test
    fun directHomeDeleteAndCancelWorkWithoutPreviousListEntry() {
        val id = seed()
        openRecent(id)
        composeRule.onNodeWithTag("delete_transaction_button").performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.cancel)).performClick()
        assertEquals(1, runBlocking { repository.observeTransactions().first().size })
        composeRule.onNodeWithTag("delete_transaction_button").performClick()
        composeRule.onNodeWithTag("confirm_delete_button").performClick()
        awaitRows(0)
        composeRule.onNodeWithTag("transactions_screen").assertExists()
        Espresso.pressBack()
        composeRule.onNodeWithTag("home_empty").performScrollTo().assertExists()
        awaitNet(0)
    }

    @Test
    fun missingRecordOpenedFromHomeCanReturnToTransactionsWithoutPreviousList() {
        val id = seed()
        openRecent(id)
        runBlocking { repository.delete(id) }
        val returnLabel = composeRule.activity.getString(R.string.back_to_transactions)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(returnLabel).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(returnLabel).performClick()
        composeRule.onNodeWithTag("transactions_screen").assertExists()
        assertTrue(home.uiState.value.isEmpty)
    }

    @Test
    fun cancelAddAndEditDoNotWriteAndBackReturnsToInvoker() {
        composeRule.onNodeWithTag("home_add").performClick()
        composeRule.onNodeWithTag("amount_input").performTextInput("123")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.onNodeWithTag("home_greeting").assertExists()
        assertTrue(runBlocking { repository.observeTransactions().first().isEmpty() })
        val id = seed()
        openRecent(id)
        composeRule.onNodeWithTag("edit_transaction_button").performClick()
        composeRule.runOnIdle { transactions.setAmount("999") }
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        composeRule.onNodeWithTag("edit_transaction_button").assertExists()
        assertEquals(100L, runBlocking { repository.observeTransactions().first().single().amountVnd })
        Espresso.pressBack()
        composeRule.onNodeWithTag("home_greeting").assertExists()
    }

    @Test
    fun viewAllSelectsHomeMonthClearsTransactionFiltersAndBackReturnsHome() {
        seed()
        val month = home.uiState.value.selectedMonth
        repeat(5) {
            composeRule.runOnIdle {
                transactions.setQuery("no match")
                transactions.setFilter(TransactionFilter.INCOME)
                transactions.selectMonth(month.minusMonths(2))
                transactions.applyFilters("Custom", TransactionDateRange(month.atDay(1), month.atDay(2)))
            }
            // List filters do not affect the Home snapshot.
            assertEquals(100L, home.uiState.value.summary?.expenseVnd)
            composeRule.onNodeWithTag("home_view_all").performScrollTo().performClick()
            composeRule.onNodeWithTag("transactions_screen").assertExists()
            assertEquals(month, transactions.uiState.value.selectedMonth)
            assertEquals("", transactions.uiState.value.query)
            assertEquals(TransactionFilter.ALL, transactions.uiState.value.filter)
            assertNull(transactions.uiState.value.selectedCategory)
            assertNull(transactions.uiState.value.dateRange)
            Espresso.pressBack()
            composeRule.onNodeWithTag("home_greeting").assertExists()
            composeRule.onNodeWithTag("transactions_screen").assertDoesNotExist()
        }
    }

    @Test
    fun selectedMonthSurvivesActivityRecreationAndRepeatedNavigation() {
        composeRule.runOnIdle { home.changeMonth(-3) }
        val selected = home.uiState.value.selectedMonth
        composeRule.activityRule.scenario.recreate()
        awaitReady()
        assertEquals(selected, home.uiState.value.selectedMonth)
        repeat(3) {
            composeRule.onNodeWithTag("home_view_all").performScrollTo().performClick()
            Espresso.pressBack()
            composeRule.onNodeWithTag("home_greeting").assertExists()
        }
        composeRule.onNodeWithTag("home_add").performClick()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back)).performClick()
        assertEquals(selected, home.uiState.value.selectedMonth)
    }

    private fun seed(date: LocalDate = home.uiState.value.selectedMonth.atDay(15)): Long {
        val id = runBlocking { repository.add(TransactionDraft(TransactionType.EXPENSE, 100, "Food", date, "Lunch")) }
        composeRule.waitUntil(5_000) { home.uiState.value.recentTransactions.any { it.id == id } }
        return id
    }

    private fun openRecent(id: Long) {
        composeRule.onNodeWithTag("transaction_row_$id").performScrollTo().performClick()
        composeRule.onNodeWithTag("edit_transaction_button").assertExists()
    }

    private fun awaitRows(count: Int) {
        composeRule.waitUntil(5_000) { transactions.uiState.value.transactions.size == count && !transactions.uiState.value.form.isSaving }
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithTag("transactions_screen").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun awaitReady() { composeRule.waitUntil(5_000) {
        home.uiState.value.loadStatus == HomeLoadStatus.READY &&
            home.uiState.value.budgetLoadStatus == HomeLoadStatus.READY
    } }
    private fun awaitNet(amount: Long) {
        composeRule.waitUntil(5_000) { home.uiState.value.summary?.netVnd == amount }
        composeRule.onNodeWithTag("home_summary").assertExists()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestDatabaseModule {
        @Provides @Singleton
        fun database(@ApplicationContext context: Context): ExpenseManagerDatabase =
            Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java).build()
        @Provides fun dao(database: ExpenseManagerDatabase): TransactionDao = database.transactionDao()

        @Provides @Singleton
        fun budgets(): MonthlyBudgetRepository = object : MonthlyBudgetRepository {
            private val limits = MutableStateFlow<Map<YearMonth, Long>>(emptyMap())
            override fun observeLimits() = limits
            override suspend fun setLimit(month: YearMonth, amountVnd: Long?) {
                limits.value = if (amountVnd == null) limits.value - month else limits.value + (month to amountVnd)
            }
        }
    }
}
