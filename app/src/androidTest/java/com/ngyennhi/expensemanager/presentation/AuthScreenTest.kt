package com.ngyennhi.expensemanager.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.presentation.auth.*
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun vietnameseValidationAndNetworkErrorAreLocalized() = localizedValidation("vi", "Đăng ký")
    @Test fun englishValidationAndNetworkErrorAreLocalized() = localizedValidation("en", "Register")

    private fun localizedValidation(language: String, title: String) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val localized = base.createConfigurationContext(config)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides config) {
                PersonalExpenseManagerTheme(darkTheme = language == "en") {
                    AuthScreen(AuthUiState(isInitialized = true, registering = true,
                        validation = AuthValidation(invalidEmail = true, weakPassword = true, passwordMismatch = true),
                        error = AuthError.NETWORK), AuthActions(), {})
                }
            }
        }
        compose.onAllNodesWithText(title).assertCountEquals(2)
        listOf(R.string.auth_local_data_summary, R.string.auth_network_error, R.string.auth_invalid_email,
            R.string.auth_password_minimum, R.string.auth_password_mismatch).forEach {
            compose.onNodeWithText(localized.getString(it)).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("auth_password").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithTag("auth_confirmation").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        compose.onNodeWithTag("auth_guest").performScrollTo().assertIsEnabled()
    }

    @Test fun loadingBlocksSubmitModeChangesAndGuestNavigationAtLargeText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                PersonalExpenseManagerTheme(darkTheme = true) {
                    AuthScreen(AuthUiState(isInitialized = true, isSubmitting = true), AuthActions(), {})
                }
            }
        }
        compose.onNodeWithTag("auth_loading").performScrollTo().assertIsDisplayed()
        listOf("auth_email", "auth_password", "auth_submit", "auth_switch_mode", "auth_guest").forEach {
            compose.onNodeWithTag(it).performScrollTo().assertIsNotEnabled()
        }
    }

    @Test fun passwordsClearWhenSwitchingModeAndLeavingTheForm() {
        var state by mutableStateOf(AuthUiState(isInitialized = true))
        var shown by mutableStateOf(true)
        compose.setContent {
            PersonalExpenseManagerTheme {
                if (shown) AuthScreen(state, AuthActions(onRegistering = { state = state.copy(registering = it) }), {})
            }
        }
        compose.onNodeWithTag("auth_password").performScrollTo().performTextInput("private-password")
        compose.onNodeWithTag("auth_switch_mode").performScrollTo().performClick()
        compose.onNodeWithTag("auth_password").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithTag("auth_password").performScrollTo().performTextInput("another-password")
        compose.runOnIdle { shown = false }
        compose.runOnIdle { shown = true }
        compose.onNodeWithTag("auth_password").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
    }

    @Test fun modeTabsExposeSelectionAndSwitchToRegistration() {
        var state by mutableStateOf(AuthUiState(isInitialized = true))
        compose.setContent {
            PersonalExpenseManagerTheme {
                AuthScreen(state, AuthActions(onRegistering = { state = state.copy(registering = it) }), {})
            }
        }
        compose.onNodeWithTag("auth_selected_mode").assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Tab))
        compose.onNodeWithTag("auth_switch_mode").assertIsNotSelected().performClick()
        compose.onNodeWithTag("auth_confirmation").assertExists()
        compose.onNodeWithTag("auth_selected_mode").assertIsSelected()
        assertTrue(state.registering)
    }

    @Test fun accountShowsRealEmailAndRequiresLogoutConfirmation() {
        var logouts = 0
        compose.setContent {
            PersonalExpenseManagerTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AccountCard(AuthUiState(user = AuthUser("a", "a@example.com"), isInitialized = true), {}, { logouts++ }, {})
                }
            }
        }
        compose.onNodeWithText("a@example.com").assertIsDisplayed()
        compose.onNodeWithTag("auth_local_data_notice").assertIsDisplayed()
        compose.onNodeWithTag("settings_logout").performScrollTo().performClick()
        assertEquals(0, logouts)
        compose.onNodeWithTag("auth_confirm_logout").assertIsDisplayed().performClick()
        assertEquals(1, logouts)
    }
}
