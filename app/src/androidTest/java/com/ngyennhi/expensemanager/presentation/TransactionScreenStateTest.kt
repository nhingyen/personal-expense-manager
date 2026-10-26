package com.ngyennhi.expensemanager.presentation

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.transaction.TransactionFilter
import com.ngyennhi.expensemanager.presentation.transaction.TransactionListScreen
import com.ngyennhi.expensemanager.presentation.transaction.TransactionUiState
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionScreenStateTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun loadingEmptyFilteredAndErrorHaveDistinctActions() {
        var state by mutableStateOf(TransactionUiState())
        var addCalls = 0
        var retryCalls = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            PersonalExpenseManagerTheme {
                TransactionListScreen(
                    state = state,
                    onQueryChange = {},
                    onFilterChange = {},
                    onApplyFilters = { _, _ -> },
                    onMonthChange = {},
                    onResetFilters = {},
                    onAdd = { addCalls++ },
                    onOpenRecord = {},
                    onRetry = { retryCalls++ },
                )
            }
        }

        composeRule.onNodeWithTag("transactions_loading").assertExists()
        composeRule.runOnIdle { state = state.copy(loadStatus = LoadStatus.READY) }
        composeRule.onNodeWithText(context.getString(R.string.empty_transactions_title)).assertExists()
        composeRule.onNodeWithTag("add_transaction_button").performClick()
        assertEquals(1, addCalls)

        composeRule.runOnIdle { state = state.copy(query = "missing", filter = TransactionFilter.EXPENSE) }
        composeRule.onNodeWithText(context.getString(R.string.no_matches_title)).assertExists()
        composeRule.runOnIdle { state = state.copy(loadStatus = LoadStatus.ERROR) }
        composeRule.onNodeWithText(context.getString(R.string.load_transactions_error)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertEquals(1, retryCalls)
    }
}
