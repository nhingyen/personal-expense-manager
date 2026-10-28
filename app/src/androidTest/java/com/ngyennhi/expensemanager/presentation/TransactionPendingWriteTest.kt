package com.ngyennhi.expensemanager.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.transaction.*
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionPendingWriteTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun pendingSaveBlocksFieldsAndBothBackActionsThenUnlocksAfterFailure() {
        var state by mutableStateOf(TransactionUiState(loadStatus = LoadStatus.READY,
            form = TransactionFormState(amountInput = "100", category = "Food", isSaving = true)))
        var backs = 0
        compose.setContent {
            PersonalExpenseManagerTheme {
                TransactionFormScreen(state, { backs++ }, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithTag("amount_input").assertIsNotEnabled()
        compose.onNodeWithTag("category_input").assertIsNotEnabled()
        compose.onNodeWithTag("date_input").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("note_input").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("save_transaction_button").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        Espresso.pressBack()
        compose.onNodeWithTag("amount_input").assertExists()
        assertEquals(0, backs)

        compose.runOnIdle { state = state.copy(form = state.form.copy(isSaving = false), actionError = ActionError.SAVE_FAILED) }
        compose.onNodeWithTag("save_transaction_button").assertIsEnabled()
        compose.onNodeWithTag("amount_input").performScrollTo().assertTextContains("100", substring = true).assertIsEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assertEquals(1, backs)
    }

    @Test fun pendingDeleteBlocksEditAndBackThenUnlocksAfterFailure() {
        val record = Transaction(7, TransactionType.EXPENSE, 100, "Food", LocalDate.of(2024, 2, 29), null)
        var state by mutableStateOf(TransactionUiState(loadStatus = LoadStatus.READY,
            transactions = listOf(record), selectedId = record.id, isDeleting = true))
        var backs = 0
        var edits = 0
        compose.setContent {
            PersonalExpenseManagerTheme {
                TransactionDetailScreen(state, { backs++ }, {}, { edits++ }, {}, {})
            }
        }
        compose.onNodeWithTag("edit_transaction_button").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("delete_transaction_button").assertIsNotEnabled()
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        Espresso.pressBack()
        compose.onNodeWithTag("edit_transaction_button").assertExists()
        assertEquals(0, backs)
        assertEquals(0, edits)
        compose.runOnIdle { state = state.copy(isDeleting = false, actionError = ActionError.DELETE_FAILED) }
        compose.onNodeWithTag("edit_transaction_button").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, edits)
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assertEquals(1, backs)
    }
}
