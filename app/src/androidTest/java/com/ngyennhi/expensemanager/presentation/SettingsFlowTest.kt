package com.ngyennhi.expensemanager.presentation

import android.content.Context
import android.content.ContentValues
import android.content.res.Configuration
import android.os.Build
import android.app.LocaleManager
import android.app.UiModeManager
import android.os.LocaleList
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.MainActivity
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.local.TransactionDao
import com.ngyennhi.expensemanager.data.repository.AndroidSettingsRepository
import com.ngyennhi.expensemanager.di.SettingsModule
import com.ngyennhi.expensemanager.di.TransactionDatabaseModule
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.SettingsRepository
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.presentation.home.HomeViewModel
import com.ngyennhi.expensemanager.presentation.settings.SettingsViewModel
import com.ngyennhi.expensemanager.presentation.statistics.StatisticsViewModel
import com.ngyennhi.expensemanager.presentation.transaction.*
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
@UninstallModules(TransactionDatabaseModule::class, SettingsModule::class)
@RunWith(AndroidJUnit4::class)
class SettingsFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var database: ExpenseManagerDatabase
    @Inject lateinit var transactions: TransactionRepository
    @Inject lateinit var settings: SettingsRepository
    private var originalLocales = LocaleListCompat.getEmptyLocaleList()
    private val vm get() = ViewModelProvider(compose.activity)[SettingsViewModel::class.java]
    private val transactionVm get() = ViewModelProvider(compose.activity)[TransactionViewModel::class.java]

    @Before fun setup() {
        hilt.inject()
        compose.runOnUiThread { originalLocales = AppCompatDelegate.getApplicationLocales() }
        compose.waitUntil(5_000) { vm.uiState.value.isInitialized && !vm.uiState.value.isLoading }
        runBlocking { settings.setTheme(AppTheme.SYSTEM) }
    }

    @After fun cleanup() {
        // Restore after closing the scenario; recreating a focused editor during Compose
        // teardown can leave the test frame clock waiting on a disposed dialog root.
        compose.activityRule.scenario.close()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            if (Build.VERSION.SDK_INT >= 33) {
                InstrumentationRegistry.getInstrumentation().targetContext
                    .getSystemService(LocaleManager::class.java).applicationLocales =
                    LocaleList.forLanguageTags(originalLocales.toLanguageTags())
            } else {
                AppCompatDelegate.setApplicationLocales(originalLocales)
            }
        }
        database.close()
    }

    @Test fun languageThemeRecreationAndNavigationRetainStateAndRoomRecords() {
        val id = runBlocking { transactions.add(TransactionDraft(TransactionType.EXPENSE, 123456,
            "Food", LocalDate.now(), "Original saved note")) }
        compose.waitUntil(5_000) { transactionVm.uiState.value.transactions.size == 1 }
        val saved = runBlocking { transactions.observeTransactions().first() }
        val home = ViewModelProvider(compose.activity)[HomeViewModel::class.java]
        val statistics = ViewModelProvider(compose.activity)[StatisticsViewModel::class.java]
        compose.runOnIdle {
            home.changeMonth(-2)
            statistics.changeMonth(-3)
            transactionVm.changeMonth(-1)
            transactionVm.setQuery("saved")
            transactionVm.setFilter(TransactionFilter.EXPENSE)
            transactionVm.applyFilters("Food", TransactionDateRange(LocalDate.now().minusDays(3), LocalDate.now()))
            transactionVm.startAdd()
            transactionVm.setAmount("999")
            transactionVm.setCategory("Draft category")
            transactionVm.setNote("Draft note")
        }
        val before = transactionVm.uiState.value
        val homeMonth = home.uiState.value.selectedMonth
        val statisticsMonth = statistics.uiState.value.selectedMonth
        compose.onNodeWithTag("nav_settings").performClick()
        for (language in listOf(AppLanguage.ENGLISH, AppLanguage.VIETNAMESE, AppLanguage.SYSTEM)) {
            selectLanguage(language)
            compose.onNodeWithTag("nav_settings").assertIsSelected()
            assertEquals(before.form, transactionVm.uiState.value.form)
            assertEquals(before.query, transactionVm.uiState.value.query)
            assertEquals(before.filter, transactionVm.uiState.value.filter)
            assertEquals(before.selectedMonth, transactionVm.uiState.value.selectedMonth)
            assertEquals(before.selectedCategory, transactionVm.uiState.value.selectedCategory)
            assertEquals(before.dateRange, transactionVm.uiState.value.dateRange)
            assertEquals(homeMonth, home.uiState.value.selectedMonth)
            assertEquals(statisticsMonth, statistics.uiState.value.selectedMonth)
            assertEquals(saved, runBlocking { transactions.observeTransactions().first() })
        }
        selectTheme(AppTheme.DARK)
        compose.runOnIdle {
            assertFalse(WindowCompat.getInsetsController(compose.activity.window,
                compose.activity.window.decorView).isAppearanceLightNavigationBars)
        }
        val dark = compose.onNodeWithTag("settings_screen").captureToImage().toPixelMap()[0, 0]
        assertEquals(Color(0xFF15121B), dark)
        selectTheme(AppTheme.LIGHT)
        compose.runOnIdle {
            assertTrue(WindowCompat.getInsetsController(compose.activity.window,
                compose.activity.window.decorView).isAppearanceLightNavigationBars)
        }
        assertEquals(Color(0xFFFAF9FC), compose.onNodeWithTag("settings_screen").captureToImage().toPixelMap()[0, 0])
        selectTheme(AppTheme.SYSTEM)
        assertEquals(before.form, transactionVm.uiState.value.form)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("nav_settings").assertIsSelected()
        compose.onNodeWithTag("theme_SYSTEM").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("language_SYSTEM").performScrollTo().assertIsSelected()
        assertEquals(saved.single().id, id)
        assertEquals(saved, runBlocking { transactions.observeTransactions().first() })
        compose.onNodeWithTag("settings_wallets").performScrollTo().performClick()
        compose.onNodeWithTag("wallet_list").assertExists()
        Espresso.pressBack()
        compose.onNodeWithTag("nav_settings").assertIsSelected()
        Espresso.pressBack()
        compose.onNodeWithTag("nav_home").assertIsSelected()
    }

    @Test fun explicitSelectionsAndInstalledVersionSurviveRecreation() {
        compose.onNodeWithTag("nav_settings").performClick()
        selectLanguage(AppLanguage.ENGLISH)
        selectTheme(AppTheme.DARK)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("language_ENGLISH").performScrollTo().assertIsSelected()
        compose.onNodeWithText("Language").assertExists()
        compose.onNodeWithTag("theme_DARK").performScrollTo().assertIsSelected()
        val version = compose.activity.packageManager.getPackageInfo(compose.activity.packageName, 0).versionName
        compose.onNodeWithTag("settings_version").performScrollTo().assertTextEquals("Version $version")
        selectLanguage(AppLanguage.VIETNAMESE)
        compose.onNodeWithText(compose.activity.getString(R.string.settings_language)).assertExists()
    }

    @Test fun changesWhileTransactionEditorIsOpenRetainRouteDraftAndDateDialog() {
        compose.onNodeWithTag("nav_add").performClick()
        compose.onNodeWithTag("amount_input").performTextInput("789")
        compose.onNodeWithTag("category_input").performTextInput("Draft category")
        Espresso.closeSoftKeyboard()
        val before = transactionVm.uiState.value.form
        compose.onNodeWithTag("date_input").performScrollTo().performClick()
        compose.onNodeWithTag("transaction_date_picker").assertExists()
        compose.runOnIdle { vm.setTheme(AppTheme.DARK) }
        compose.waitUntil(5_000) { vm.uiState.value.theme == AppTheme.DARK && !vm.uiState.value.isSaving }
        val language = if (vm.uiState.value.language == AppLanguage.ENGLISH) AppLanguage.VIETNAMESE else AppLanguage.ENGLISH
        compose.runOnIdle { vm.setLanguage(language) }
        compose.waitUntil(10_000) { vm.uiState.value.language == language && !vm.uiState.value.isSaving }
        compose.onNodeWithTag("transaction_date_picker").assertExists()
        compose.onNodeWithTag("cancel_transaction_date").performClick()
        compose.onNodeWithTag("amount_input").assertTextContains("789", substring = true)
        assertEquals(before, transactionVm.uiState.value.form)
        compose.onNodeWithTag("app_bottom_bar").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("amount_input").assertTextContains("789", substring = true)
        assertEquals(before, transactionVm.uiState.value.form)
        assertTrue(runBlocking { transactions.observeTransactions().first().isEmpty() })
    }

    @Test @SdkSuppress(minSdkVersion = 33)
    fun androidPerAppLanguageChangeIsReflectedWithoutOverwritingIt() {
        compose.onNodeWithTag("nav_settings").performClick()
        selectLanguage(AppLanguage.VIETNAMESE)
        if (Build.VERSION.SDK_INT >= 33) {
            compose.runOnUiThread {
                compose.activity.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("en")
            }
        }
        compose.waitUntil(10_000) { vm.uiState.value.language == AppLanguage.ENGLISH }
        compose.onNodeWithTag("language_ENGLISH").assertIsSelected()
        compose.onNodeWithText("Language").assertExists()
        selectLanguage(AppLanguage.SYSTEM)
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    fun captureSettingsForReview() {
        compose.onNodeWithTag("nav_settings").performClick()
        selectLanguage(AppLanguage.VIETNAMESE)
        selectTheme(AppTheme.LIGHT)
        capture("settings-vi-light")
        selectLanguage(AppLanguage.ENGLISH)
        selectTheme(AppTheme.DARK)
        capture("settings-en-dark")
    }

    @Test @SdkSuppress(minSdkVersion = 29)
    fun systemThemeTracksDeviceWhileExplicitChoicesStayFixed() {
        compose.onNodeWithTag("nav_settings").performClick()
        val manager = compose.activity.getSystemService(UiModeManager::class.java)
        val original = manager.nightMode
        fun systemNight(mode: String) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("cmd uimode night $mode").use { descriptor ->
                    java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
                }
        }
        fun awaitAppliedAppearance(dark: Boolean) {
            compose.waitUntil(5_000) {
                WindowCompat.getInsetsController(compose.activity.window, compose.activity.window.decorView)
                    .isAppearanceLightStatusBars == !dark
            }
        }
        fun awaitNight(night: Int) {
            // Wait for Activity configuration before asking Compose for a screenshot;
            // synchronizing an old root while it is being recreated can stall the frame clock.
            compose.waitUntil(10_000) {
                compose.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == night
            }
        }
        try {
            selectTheme(AppTheme.SYSTEM)
            systemNight("yes")
            awaitNight(Configuration.UI_MODE_NIGHT_YES)
            assertTrue(vm.uiState.value.theme.isDark(true))
            awaitAppliedAppearance(dark = true)
            compose.onNodeWithTag("nav_settings").assertIsSelected()
            selectTheme(AppTheme.LIGHT)
            awaitAppliedAppearance(dark = false)
            systemNight("no")
            awaitNight(Configuration.UI_MODE_NIGHT_NO)
            selectTheme(AppTheme.DARK)
            awaitAppliedAppearance(dark = true)
            selectTheme(AppTheme.SYSTEM)
            assertFalse(vm.uiState.value.theme.isDark(false))
            awaitAppliedAppearance(dark = false)
        } finally {
            systemNight(when (original) {
                UiModeManager.MODE_NIGHT_YES -> "yes"
                UiModeManager.MODE_NIGHT_NO -> "no"
                else -> "auto"
            })
        }
    }

    @androidx.annotation.RequiresApi(29)
    private fun capture(name: String) {
        compose.onNodeWithTag("settings_screen").performTouchInput { swipeDown() }
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // System bar icon updates are asynchronous outside Compose's idling resources.
        instrumentation.uiAutomation.waitForIdle(500, 5_000)
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        val resolver = instrumentation.targetContext.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "task010-$name.png")
            put(MediaStore.Downloads.MIME_TYPE, "image/png")
        }))
        requireNotNull(resolver.openOutputStream(uri)).use {
            assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        screenshot.recycle()
    }

    private fun selectLanguage(language: AppLanguage) {
        compose.onNodeWithTag("language_${language.name}").performScrollTo().performClick()
        compose.waitUntil(10_000) { vm.uiState.value.language == language && !vm.uiState.value.isSaving }
        compose.onNodeWithTag("language_${language.name}").performScrollTo().assertIsSelected()
        if (language != AppLanguage.SYSTEM) {
            compose.waitUntil(10_000) { compose.activity.resources.configuration.locales[0].language == language.tag }
        }
    }

    private fun selectTheme(theme: AppTheme) {
        compose.onNodeWithTag("theme_${theme.name}").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.uiState.value.theme == theme && !vm.uiState.value.isSaving }
        compose.onNodeWithTag("theme_${theme.name}").assertIsSelected()
    }

    @Module @InstallIn(SingletonComponent::class)
    object Storage {
        @Provides @Singleton fun database(@ApplicationContext context: Context) =
            Room.inMemoryDatabaseBuilder(context, ExpenseManagerDatabase::class.java)
                .addCallback(ExpenseManagerDatabase.SEED_DEFAULT_WALLET).build()
        @Provides fun dao(database: ExpenseManagerDatabase): TransactionDao = database.transactionDao()
        @Provides @Singleton fun settings(@ApplicationContext context: Context): SettingsRepository =
            AndroidSettingsRepository(context.getSharedPreferences("task010_test", Context.MODE_PRIVATE))
    }
}
