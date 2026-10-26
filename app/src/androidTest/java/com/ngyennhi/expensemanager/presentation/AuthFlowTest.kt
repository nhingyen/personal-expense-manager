package com.ngyennhi.expensemanager.presentation

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngyennhi.expensemanager.MainActivity
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.local.TransactionDao
import com.ngyennhi.expensemanager.data.repository.AndroidSettingsRepository
import com.ngyennhi.expensemanager.data.repository.PreferencesMonthlyBudgetRepository
import com.ngyennhi.expensemanager.di.*
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.*
import com.ngyennhi.expensemanager.domain.usecase.ValidateAuthCredentialsUseCase
import com.ngyennhi.expensemanager.presentation.auth.AuthViewModel
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.wallet.WalletViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.*
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(AuthModule::class, TransactionDatabaseModule::class, SettingsModule::class, MonthlyBudgetStorageModule::class)
@RunWith(AndroidJUnit4::class)
class AuthFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var database: ExpenseManagerDatabase
    @Inject lateinit var transactions: TransactionRepository
    @Inject lateinit var wallets: WalletRepository
    @Inject lateinit var budgets: MonthlyBudgetRepository
    @Inject lateinit var auth: FakeAuth
    private val vm get() = ViewModelProvider(compose.activity)[AuthViewModel::class.java]

    @Before fun setup() {
        hilt.inject()
        compose.waitUntil(5_000) { vm.uiState.value.isInitialized }
    }

    @After fun cleanup() { compose.activityRule.scenario.close(); database.close() }

    @Test fun guestLoginLogoutAndSecondAccountKeepRoomWalletsBalancesAndBudgetsUnchanged() {
        val month = YearMonth.now()
        runBlocking {
            val bank = wallets.add(WalletDraft("Bank", WalletType.BANK, 1000000))
            transactions.add(TransactionDraft(TransactionType.EXPENSE, 85000, "Food", LocalDate.now(), "Guest record", bank))
            budgets.setLimit(month, 500000)
        }
        val records = runBlocking { transactions.observeTransactions().first() }
        val savedWallets = runBlocking { wallets.observeWallets().first() }
        val savedBudgets = runBlocking { budgets.observeLimits().first() }
        val walletVm = ViewModelProvider(compose.activity)[WalletViewModel::class.java]
        compose.waitUntil(5_000) { walletVm.uiState.value.status == LoadStatus.READY && walletVm.uiState.value.transactions.size == 1 }
        val balances = walletVm.uiState.value.balances
        compose.onNodeWithTag("home_content").assertExists()
        openSettings()
        compose.onNodeWithTag("settings_login").performScrollTo().assertIsEnabled()
        login("a@example.com", registering = true)
        assertData(records, savedWallets, savedBudgets, balances, walletVm)
        compose.onNodeWithText("a@example.com").assertIsDisplayed()
        // Recreating the host restores the secondary/primary navigation and observed session.
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("a@example.com").assertIsDisplayed()
        compose.onNodeWithTag("settings_logout").performScrollTo().performClick()
        compose.onNodeWithTag("auth_confirm_logout").performClick()
        compose.waitUntil(5_000) { vm.uiState.value.user == null && !vm.uiState.value.isSubmitting }
        compose.onNodeWithTag("settings_login").assertIsDisplayed()
        assertData(records, savedWallets, savedBudgets, balances, walletVm)
        login("b@example.com", registering = false)
        compose.onNodeWithText("b@example.com").assertIsDisplayed()
        assertData(records, savedWallets, savedBudgets, balances, walletVm)
    }

    @Test fun networkFailureCanRetryAndContinueAsGuestReturnsToSettingsWithoutChangingData() {
        openSettings()
        compose.onNodeWithTag("settings_login").performScrollTo().performClick()
        compose.onNodeWithTag("app_bottom_bar").assertDoesNotExist()
        compose.onNodeWithTag("auth_email").performTextInput("a@example.com")
        compose.onNodeWithTag("auth_password").performTextInput("secret")
        hideKeyboard()
        auth.failure = AuthError.NETWORK
        compose.onNodeWithTag("auth_submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.error == AuthError.NETWORK }
        compose.onNodeWithTag("auth_guest").performScrollTo().performClick()
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        assertNull(vm.uiState.value.user)
        assertEquals("", vm.uiState.value.email)
        assertTrue(runBlocking { transactions.observeTransactions().first().isEmpty() })
        auth.failure = null
        login("a@example.com", registering = false)
        compose.onNodeWithText("a@example.com").assertIsDisplayed()
    }

    @Test fun googleSessionAndLogoutKeepLocalFinancialDataAvailableToGuest() {
        val month = YearMonth.now()
        runBlocking {
            val bank = wallets.add(WalletDraft("Google flow wallet", WalletType.BANK, 1000000))
            transactions.add(TransactionDraft(TransactionType.EXPENSE, 85000, "Food", LocalDate.now(), "Local record", bank))
            budgets.setLimit(month, 500000)
        }
        val records = runBlocking { transactions.observeTransactions().first() }
        val savedWallets = runBlocking { wallets.observeWallets().first() }
        val savedBudgets = runBlocking { budgets.observeLimits().first() }
        val walletVm = ViewModelProvider(compose.activity)[WalletViewModel::class.java]
        compose.waitUntil(5_000) { walletVm.uiState.value.status == LoadStatus.READY && walletVm.uiState.value.transactions.size == 1 }
        val balances = walletVm.uiState.value.balances
        runBlocking { vm.loginWithGoogle { "fake-id-token" } }
        compose.waitUntil(5_000) { vm.uiState.value.user?.email == "google@example.com" }
        assertData(records, savedWallets, savedBudgets, balances, walletVm)
        compose.runOnUiThread { vm.logout() }
        compose.waitUntil(5_000) { vm.uiState.value.user == null && !vm.uiState.value.isSubmitting }
        assertData(records, savedWallets, savedBudgets, balances, walletVm)
    }

    private fun openSettings() { compose.onNodeWithTag("nav_settings").performClick() }

    private fun login(email: String, registering: Boolean) {
        compose.onNodeWithTag("settings_login").performScrollTo().performClick()
        if (registering) compose.onNodeWithTag("auth_switch_mode").performScrollTo().performClick()
        compose.onNodeWithTag("auth_email").performScrollTo().performTextInput(email)
        compose.onNodeWithTag("auth_password").performScrollTo().performTextInput("secret")
        if (registering) compose.onNodeWithTag("auth_confirmation").performScrollTo().performTextInput("secret")
        hideKeyboard()
        compose.onNodeWithTag("auth_submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.user?.email == email && !vm.uiState.value.isSubmitting }
        compose.onNodeWithTag("settings_screen").assertIsDisplayed()
        compose.onNodeWithTag("auth_screen").assertDoesNotExist()
    }

    private fun assertData(records: List<Transaction>, savedWallets: List<Wallet>, savedBudgets: Map<YearMonth, Long>,
        balances: List<WalletBalance>, walletVm: WalletViewModel) {
        assertEquals(records, runBlocking { transactions.observeTransactions().first() })
        assertEquals(savedWallets, runBlocking { wallets.observeWallets().first() })
        assertEquals(savedBudgets, runBlocking { budgets.observeLimits().first() })
        assertEquals(balances, walletVm.uiState.value.balances)
    }

    private fun hideKeyboard() {
        compose.runOnUiThread {
            val window = compose.activity.window
            WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        }
    }

    class FakeAuth : AuthRepository {
        val user = MutableStateFlow<AuthUser?>(null)
        var failure: AuthError? = null
        override fun observeUser() = user
        override suspend fun login(email: String, password: String) {
            failure?.let { throw AuthFailure(it) }
            user.value = AuthUser(email, email)
        }
        override suspend fun register(email: String, password: String) = login(email, password)
        override suspend fun loginWithGoogle(idToken: String) {
            failure?.let { throw AuthFailure(it) }
            user.value = AuthUser("google", "google@example.com")
        }
        override suspend fun logout() { user.value = null }
    }

    @Module @InstallIn(SingletonComponent::class)
    object Storage {
        @Provides @Singleton fun database(@ApplicationContext context: Context) =
            Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java)
                .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()
        @Provides fun dao(database: ExpenseManagerDatabase): TransactionDao = database.transactionDao()
        @Provides @Singleton fun fakeAuth() = FakeAuth()
        @Provides fun auth(fake: FakeAuth): AuthRepository = fake
        @Provides fun validation() = ValidateAuthCredentialsUseCase()
        @Provides @Singleton fun settings(@ApplicationContext context: Context): SettingsRepository =
            AndroidSettingsRepository(context.getSharedPreferences("task011_settings", Context.MODE_PRIVATE))
        @Provides @Singleton fun budgets(@ApplicationContext context: Context): MonthlyBudgetRepository =
            PreferencesMonthlyBudgetRepository(context.getSharedPreferences("task011_budgets", Context.MODE_PRIVATE).apply { edit().clear().commit() })
    }
}
