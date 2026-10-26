package com.ngyennhi.expensemanager.presentation

import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import androidx.test.filters.SdkSuppress
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.MainActivity
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.data.local.*
import com.ngyennhi.expensemanager.di.TransactionDatabaseModule
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.*
import com.ngyennhi.expensemanager.presentation.home.HomeViewModel
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.wallet.WalletViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.*
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(TransactionDatabaseModule::class)
@RunWith(AndroidJUnit4::class)
class WalletFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var database: ExpenseManagerDatabase
    @Inject lateinit var wallets: WalletRepository
    @Inject lateinit var transactions: TransactionRepository
    private val vm get() = ViewModelProvider(compose.activity)[WalletViewModel::class.java]
    @Before fun setup() {
        hiltRule.inject()
        compose.waitUntil(5_000) { vm.uiState.value.status == LoadStatus.READY }
    }
    @After fun cleanup() { database.close() }

    @Test fun createEditSelectMoveAndDeleteWalletWithoutChangingGlobalSpending() {
        openWallets()
        compose.onNodeWithTag("wallet_add_button").performScrollTo().performClick()
        compose.onNodeWithTag("wallet_name_input").performTextInput("Bank")
        compose.onNodeWithTag("wallet_type_BANK").performClick()
        opening("1000")
        compose.onNodeWithTag("wallet_save_button").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.wallets.size == 2 }
        val bank = vm.uiState.value.wallets.last().id
        compose.onNodeWithTag("wallet_add_transaction").performScrollTo().performClick()
        compose.onNodeWithTag("amount_input").performTextInput("100")
        compose.onNodeWithTag("category_input").performTextInput("Food")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("wallet_picker").performScrollTo().performClick()
        compose.onNodeWithTag("wallet_option_1").performClick()
        compose.onNodeWithTag("wallet_picker").performClick()
        compose.onNodeWithTag("wallet_option_$bank").performClick()
        compose.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.transactions.size == 1 }
        assertEquals(bank, vm.uiState.value.transactions.single().walletId)
        val record = vm.uiState.value.transactions.single()
        openWallets()
        compose.onNodeWithTag("wallet_row_$bank").performClick()
        compose.onNodeWithTag("wallet_delete_button").performScrollTo().assertIsNotEnabled()
        assertEquals(900L, vm.uiState.value.balances.last().balanceVnd)
        compose.onNodeWithTag("wallet_edit_button").performScrollTo().performClick()
        opening("2000")
        compose.onNodeWithTag("wallet_save_button").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.balances.last().balanceVnd == 1900L }
        compose.onNodeWithTag("transaction_row_${record.id}").performScrollTo().performClick()
        compose.onNodeWithText("Bank", substring = false).assertExists()
        compose.onNodeWithTag("edit_transaction_button").performScrollTo().performClick()
        compose.onNodeWithTag("wallet_picker").performScrollTo().performClick()
        compose.onNodeWithTag("wallet_option_1").performClick()
        compose.onNodeWithTag("save_transaction_button").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.transactions.single().walletId == 1L }
        assertEquals(listOf(-100L, 2000L), vm.uiState.value.balances.map { it.balanceVnd })
        val home = ViewModelProvider(compose.activity)[HomeViewModel::class.java]
        compose.waitUntil(5_000) { home.uiState.value.summary?.expenseVnd == 100L }
        assertEquals(0L, home.uiState.value.summary!!.incomeVnd)
        openWallets(); compose.onNodeWithTag("wallet_row_$bank").performClick()
        compose.onNodeWithTag("transaction_row_${record.id}").assertDoesNotExist()
        compose.onNodeWithTag("wallet_delete_button").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithTag("wallet_confirm_delete").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.wallets.size == 1 }
        compose.onNodeWithTag("wallet_row_1").performClick()
        compose.onNodeWithTag("wallet_delete_button").performScrollTo().assertIsNotEnabled()
        assertEquals(1, runBlocking { transactions.observeTransactions().first().size })
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    fun captureWalletScreensWithIsolatedFictionalData() {
        val bank = runBlocking {
            wallets.update(Wallet(1, "Cash", WalletType.CASH, 2_000_000))
            val bankId = wallets.add(WalletDraft("Everyday bank", WalletType.BANK, 5_000_000))
            wallets.add(WalletDraft("E-wallet", WalletType.E_WALLET, 500_000))
            transactions.add(TransactionDraft(TransactionType.INCOME, 8_000_000, "Salary", LocalDate.now(), "Monthly salary", bankId))
            transactions.add(TransactionDraft(TransactionType.EXPENSE, 85_000, "Food", LocalDate.now(), "Lunch", 1))
            bankId
        }
        compose.waitUntil(5_000) { vm.uiState.value.wallets.size == 3 && vm.uiState.value.transactions.size == 2 }
        openWallets(); capture("wallets")
        compose.onNodeWithTag("wallet_row_$bank").performClick(); capture("wallet-details")
        compose.onNodeWithTag("wallet_edit_button").performScrollTo().performClick(); capture("edit-wallet")
        Espresso.pressBack()
        compose.onNodeWithTag("wallet_add_transaction").performScrollTo().performClick()
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("wallet_picker").performScrollTo()
        capture("transaction-wallet")
    }

    private fun opening(value: String) {
        compose.onNodeWithTag("wallet_opening_input").performScrollTo().performTextClearance()
        compose.onNodeWithTag("wallet_opening_input").performTextInput(value)
        Espresso.closeSoftKeyboard()
    }
    private fun openWallets() {
        // Saves return to Transactions; wallet secondary routes expose normal Back.
        compose.waitForIdle()
        compose.onNodeWithTag("nav_settings").performClick()
        compose.onNodeWithTag("settings_wallets").performClick()
        compose.onNodeWithTag("wallet_list").assertExists()
    }
    @androidx.annotation.RequiresApi(29)
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        val resolver = instrumentation.targetContext.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "task009-$name.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
        }))
        requireNotNull(resolver.openOutputStream(uri)).use {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        screenshot.recycle()
    }
    @Module @InstallIn(SingletonComponent::class)
    object DatabaseModule {
        @Provides @Singleton fun database(@ApplicationContext context: Context) =
            Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java)
                .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()
        @Provides fun transactions(database: ExpenseManagerDatabase): TransactionDao = database.transactionDao()
    }
}
