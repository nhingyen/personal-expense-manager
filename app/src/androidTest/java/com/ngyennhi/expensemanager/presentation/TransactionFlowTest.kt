package com.ngyennhi.expensemanager.presentation

import android.content.Context
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.ViewModelProvider
import com.ngyennhi.expensemanager.MainActivity
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.local.TransactionDao
import com.ngyennhi.expensemanager.di.TransactionDatabaseModule
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.transaction.TransactionViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(TransactionDatabaseModule::class)
@RunWith(AndroidJUnit4::class)
class TransactionFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var database: ExpenseManagerDatabase

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createEditCancelDeleteAndConfirmDeleteUseRoom() {
        val activity = composeRule.activity
        composeRule.onNodeWithTag("home_view_all").performScrollTo().performClick()
        composeRule.onNodeWithText(activity.getString(R.string.empty_transactions_title)).assertExists()

        composeRule.onNodeWithTag("add_transaction_button").performClick()
        composeRule.onNodeWithTag("amount_input").performTextInput("85000")
        composeRule.onNodeWithTag("category_input").performTextInput("Food")
        val viewModel = ViewModelProvider(activity)[TransactionViewModel::class.java]
        assertEquals("85000", viewModel.uiState.value.form.amountInput)
        assertEquals("Food", viewModel.uiState.value.form.category)
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("save_transaction_button").performClick()
        composeRule.waitForIdle()
        assertEquals(null, viewModel.uiState.value.form.amountError)
        assertEquals(null, viewModel.uiState.value.form.categoryError)
        assertEquals(null, viewModel.uiState.value.actionError)

        composeRule.waitUntil(5_000) {
            viewModel.uiState.value.transactions.isNotEmpty() || viewModel.uiState.value.actionError != null
        }
        assertEquals("UI state: ${viewModel.uiState.value}", 1, viewModel.uiState.value.transactions.size)
        composeRule.waitUntil(5_000) { runBlocking { database.transactionDao().observeAll().first().size == 1 } }
        val id = runBlocking { database.transactionDao().observeAll().first().single().id }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("transaction_row_$id").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("transaction_row_$id").performClick()
        composeRule.onNodeWithTag("edit_transaction_button").performClick()
        composeRule.onNodeWithTag("amount_input").performTextClearance()
        composeRule.onNodeWithTag("amount_input").performTextInput("90000")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("save_transaction_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { database.transactionDao().observeAll().first().single().amountVnd == 90_000L }
        }
        assertEquals(id, runBlocking { database.transactionDao().observeAll().first().single().id })

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("transaction_row_$id").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("transaction_row_$id").performClick()
        composeRule.onNodeWithTag("delete_transaction_button").performClick()
        composeRule.onNodeWithText(activity.getString(R.string.cancel)).performClick()
        assertEquals(1, runBlocking { database.transactionDao().observeAll().first().size })
        composeRule.onNodeWithTag("delete_transaction_button").performClick()
        composeRule.onNodeWithTag("confirm_delete_button").performClick()
        composeRule.waitUntil(5_000) { runBlocking { database.transactionDao().observeAll().first().isEmpty() } }
        composeRule.onNodeWithText(activity.getString(R.string.empty_transactions_title)).assertExists()
    }

    @Test
    fun createIncomeUsingSuggestedCategory() {
        val activity = composeRule.activity
        composeRule.onNodeWithTag("home_view_all").performScrollTo().performClick()
        composeRule.onNodeWithTag("add_transaction_button").performClick()
        composeRule.onNodeWithText(activity.getString(R.string.type_income)).performClick()
        composeRule.onNodeWithTag("amount_input").performTextInput("125000")
        Espresso.closeSoftKeyboard()
        composeRule.onNodeWithTag("choose_category_button").performClick()
        composeRule.onNodeWithText(activity.getString(R.string.category_salary)).performClick()
        composeRule.onNodeWithTag("save_transaction_button").performClick()
        composeRule.waitUntil(5_000) {
            runBlocking { database.transactionDao().observeAll().first().size == 1 }
        }
        val record = runBlocking { database.transactionDao().observeAll().first().single() }
        assertEquals(TransactionType.INCOME.name, record.type)
        assertEquals(125_000L, record.amountVnd)
        assertEquals(activity.getString(R.string.category_salary), record.category)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestDatabaseModule {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): ExpenseManagerDatabase =
            Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java).build()

        @Provides
        fun provideDao(database: ExpenseManagerDatabase): TransactionDao = database.transactionDao()
    }
}
