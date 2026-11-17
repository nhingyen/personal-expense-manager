package com.ngyennhi.expensemanager.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.category.CategoryEditorState
import com.ngyennhi.expensemanager.presentation.category.CategoryManagementScreen
import com.ngyennhi.expensemanager.presentation.category.CategoryNameError
import com.ngyennhi.expensemanager.presentation.category.CategoryUiState
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.transaction.TransactionFormScreen
import com.ngyennhi.expensemanager.presentation.transaction.TransactionUiState
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryManagementScreenTest {
    @get:Rule val compose = createComposeRule()
    private val active = Category(1, TransactionType.EXPENSE, "Travel", false)
    private val archived = Category(2, TransactionType.EXPENSE, "Old category", true)

    @Test fun listShowsActiveAndArchivedAndOpensSelectedCategory() {
        var selected: Category? = null
        compose.setContent { PersonalExpenseManagerTheme {
            CategoryManagementScreen(CategoryUiState(status = LoadStatus.READY,
                categories = listOf(active, archived)), {}, {}, { selected = it }, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("category_card_1").assertExists().performClick()
        compose.onNodeWithText("Old category").assertExists()
        assertEquals(active, selected)
    }

    @Test fun editorShowsDuplicateAndDeleteRequiresConfirmation() {
        var archives = 0; var deletes = 0
        compose.setContent { PersonalExpenseManagerTheme {
            CategoryManagementScreen(CategoryUiState(status = LoadStatus.READY,
                categories = listOf(active), editor = CategoryEditorState(active.id, active.type,
                    active.name, CategoryNameError.DUPLICATE)), {}, {}, {}, {}, {}, {}, {},
                { archives++ }, { deletes++ }, {}, {})
        } }
        compose.onNodeWithTag("category_toggle_archive").performClick()
        assertEquals(1, archives)
        compose.onNodeWithTag("category_delete").performClick()
        assertEquals(0, deletes)
        compose.onNodeWithTag("category_confirm_delete").assertExists().performClick()
        assertEquals(1, deletes)
    }

    @Test fun transactionPickerCombinesActiveCatalogAndSuggestionsButHidesArchived() {
        compose.setContent { PersonalExpenseManagerTheme {
            TransactionFormScreen(state = TransactionUiState(loadStatus = LoadStatus.READY),
                onBack = {}, onReturnToList = {}, onRetry = {}, onTypeChange = {},
                onAmountChange = {}, onCategoryChange = {}, onDateChange = {}, onNoteChange = {}, onSave = {},
                categoryState = CategoryUiState(status = LoadStatus.READY, categories = listOf(active, archived)))
        } }
        compose.onNodeWithTag("choose_category_button").performClick()
        compose.onNodeWithText("Travel").assertExists()
        compose.onNodeWithText("Old category").assertDoesNotExist()
    }
}
