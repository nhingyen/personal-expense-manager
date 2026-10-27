package com.ngyennhi.expensemanager.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngyennhi.expensemanager.MainActivity
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.local.TransactionDao
import com.ngyennhi.expensemanager.data.repository.AndroidSettingsRepository
import com.ngyennhi.expensemanager.data.repository.PreferencesMonthlyBudgetRepository
import com.ngyennhi.expensemanager.di.MonthlyBudgetStorageModule
import com.ngyennhi.expensemanager.di.SettingsModule
import com.ngyennhi.expensemanager.di.TransactionDatabaseModule
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.*
import com.ngyennhi.expensemanager.presentation.home.HomeViewModel
import com.ngyennhi.expensemanager.presentation.statistics.StatisticsViewModel
import com.ngyennhi.expensemanager.presentation.wallet.WalletViewModel
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
import java.util.UUID
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
@UninstallModules(TransactionDatabaseModule::class, MonthlyBudgetStorageModule::class, SettingsModule::class)
@RunWith(AndroidJUnit4::class)
class OfflineFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var database: ExpenseManagerDatabase
    @Inject lateinit var transactions: TransactionRepository
    @Inject lateinit var wallets: WalletRepository
    @Inject lateinit var budgets: MonthlyBudgetRepository
    private val home get() = ViewModelProvider(compose.activity)[HomeViewModel::class.java]

    @Before fun setup() { hilt.inject() }
    @After fun cleanup() { compose.activityRule.scenario.close(); database.close() }

    @Test fun homeAndFinancialTabsWorkWithoutAccountsAndRetainDataAcrossRecreation() {
        // All test writes use an in-memory Room database and isolated preferences.
        val bank = runBlocking {
            val id = wallets.add(WalletDraft("Offline bank", WalletType.BANK, 1_000_000))
            transactions.add(TransactionDraft(TransactionType.INCOME, 200_000, "Salary", LocalDate.now(), "Offline income", id))
            transactions.add(TransactionDraft(TransactionType.EXPENSE, 85_000, "Food", LocalDate.now(), "Offline expense", id))
            budgets.setLimit(YearMonth.now(), 500_000)
            id
        }
        val savedRecords = runBlocking { transactions.observeTransactions().first() }
        val savedWallets = runBlocking { wallets.observeWallets().first() }
        val savedBudgets = runBlocking { budgets.observeLimits().first() }
        compose.waitUntil(5_000) {
            home.uiState.value.summary?.incomeVnd == 200_000L &&
                home.uiState.value.summary?.expenseVnd == 85_000L && home.uiState.value.budgetLimitVnd == 500_000L
        }
        compose.onNodeWithTag("home_content").assertExists()
        compose.onNodeWithTag("nav_home").assertIsSelected()
        compose.onNodeWithTag("auth_screen").assertDoesNotExist()
        val permissions = compose.activity.packageManager.getPackageInfo(
            compose.activity.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse(permissions.contains(Manifest.permission.INTERNET))

        compose.onNodeWithTag("nav_transactions").performClick()
        savedRecords.forEach { compose.onNodeWithTag("transaction_row_${it.id}").assertExists() }
        compose.onNodeWithTag("nav_statistics").performClick()
        val statistics = ViewModelProvider(compose.activity)[StatisticsViewModel::class.java]
        compose.waitUntil(5_000) { statistics.uiState.value.statistics?.summary?.expenseVnd == 85_000L }
        assertEquals(200_000L, statistics.uiState.value.statistics!!.summary.incomeVnd)

        compose.onNodeWithTag("nav_settings").performClick()
        compose.onNodeWithTag("settings_screen").assertExists()
        compose.onNodeWithTag("settings_login").assertDoesNotExist()
        compose.onNodeWithTag("settings_logout").assertDoesNotExist()
        compose.onNodeWithTag("language_SYSTEM").performScrollTo().assertExists()
        compose.onNodeWithTag("theme_SYSTEM").performScrollTo().assertExists()
        compose.onNodeWithTag("settings_version").performScrollTo().assertTextEquals(
            compose.activity.getString(R.string.settings_version, "1.0.0"))
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("nav_settings").assertIsSelected()
        compose.onNodeWithTag("settings_wallets").performScrollTo().performClick()
        compose.onNodeWithTag("wallet_row_$bank").assertExists()
        val walletState = ViewModelProvider(compose.activity)[WalletViewModel::class.java]
        compose.waitUntil(5_000) { walletState.uiState.value.balances.any { it.wallet.id == bank } }
        assertEquals(1_115_000L, walletState.uiState.value.balances.single { it.wallet.id == bank }.balanceVnd)

        assertEquals(savedRecords, runBlocking { transactions.observeTransactions().first() })
        assertEquals(savedWallets, runBlocking { wallets.observeWallets().first() })
        assertEquals(savedBudgets, runBlocking { budgets.observeLimits().first() })
        assertEquals(500_000L, home.uiState.value.budgetLimitVnd)
    }

    @Module @InstallIn(SingletonComponent::class)
    object Storage {
        @Provides @Singleton fun database(@ApplicationContext context: Context): ExpenseManagerDatabase =
            Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java)
                .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()
        @Provides fun dao(database: ExpenseManagerDatabase): TransactionDao = database.transactionDao()
        @Provides @Singleton fun budgets(@ApplicationContext context: Context): MonthlyBudgetRepository =
            PreferencesMonthlyBudgetRepository(context.getSharedPreferences("offline_test_budgets_${UUID.randomUUID()}", Context.MODE_PRIVATE))
        @Provides @Singleton fun settings(@ApplicationContext context: Context): SettingsRepository =
            AndroidSettingsRepository(context.getSharedPreferences("offline_test_appearance_${UUID.randomUUID()}", Context.MODE_PRIVATE))
    }
}
