package com.ngyennhi.expensemanager.presentation

import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.wallet.*
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletScreenStateTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun readErrorRetryEmptyAndOverflowNeverInventAmounts() {
        var state by mutableStateOf(WalletUiState())
        var retries = 0
        compose.setContent { PersonalExpenseManagerTheme { WalletListScreen(state, {}, {}, {}, {}, { retries++ }) } }
        compose.onNodeWithTag("wallet_add_button").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(status = LoadStatus.ERROR, error = WalletError.READ) }
        compose.onNodeWithText(context.getString(R.string.wallet_read_error)).assertExists()
        compose.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertEquals(1, retries)
        compose.runOnIdle { state = WalletUiState(status = LoadStatus.READY) }
        compose.onNodeWithText(context.getString(R.string.wallet_empty)).assertExists()
        compose.onNodeWithTag("wallet_add_button").assertIsEnabled()
        compose.runOnIdle { state = state.copy(error = WalletError.OVERFLOW, wallets = listOf(Wallet(1, "Cash", WalletType.CASH, 0))) }
        compose.onNodeWithText(context.getString(R.string.wallet_overflow)).assertExists()
        compose.onNodeWithTag("wallet_row_1").assertExists()
    }
    @Test fun editorSupportsBothLocalesLargeTextValidationAndPendingWrite() {
        var locale by mutableStateOf(Locale.forLanguageTag("vi"))
        var state by mutableStateOf(WalletUiState(status = LoadStatus.READY,
            form = WalletFormState(invalidName = true, invalidOpening = true)))
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply { setLocale(locale) }
            val localized = context.createConfigurationContext(configuration)
            CompositionLocalProvider(LocalContext provides localized, LocalResources provides localized.resources,
                LocalConfiguration provides configuration, LocalDensity provides Density(LocalDensity.current.density, 1.5f)) {
                PersonalExpenseManagerTheme { WalletFormScreen(state, {}, {}, {}, {}, {}, {}) }
            }
        }
        for (language in listOf("vi", "en")) {
            compose.runOnIdle { locale = Locale.forLanguageTag(language) }
            val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) }).resources
            compose.onNodeWithText(resources.getString(R.string.wallet_name_invalid)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("wallet_type_E_WALLET").performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(resources.getString(R.string.wallet_opening_invalid)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("wallet_save_button").performScrollTo().assertIsDisplayed().assertIsEnabled()
        }
        compose.runOnIdle { state = state.copy(writing = true) }
        compose.onNodeWithTag("wallet_save_button").assertIsNotEnabled()
        compose.onNodeWithTag("wallet_name_input").assertIsNotEnabled()
    }
}
