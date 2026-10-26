package com.ngyennhi.expensemanager.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import com.ngyennhi.expensemanager.presentation.settings.SettingsScreen
import com.ngyennhi.expensemanager.presentation.settings.SettingsUiState
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun largeTextKeepsChoicesAndAboutReachableInDarkTheme() {
        var language: AppLanguage? = null
        var theme: AppTheme? = null
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                PersonalExpenseManagerTheme(darkTheme = true) {
                    SettingsScreen(SettingsUiState(isLoading = false), { language = it }, { theme = it }, {}, {})
                }
            }
        }
        compose.onNodeWithTag("language_ENGLISH").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(AppLanguage.ENGLISH, language)
        compose.onNodeWithTag("theme_DARK").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(AppTheme.DARK, theme)
        compose.onNodeWithTag("settings_version").performScrollTo().assertIsDisplayed()
    }

    @Test fun readFailureBlocksWritesAndExposesRetry() {
        var retries = 0
        var writes = 0
        compose.setContent {
            PersonalExpenseManagerTheme(darkTheme = false) {
                SettingsScreen(SettingsUiState(isLoading = false, readError = true),
                    { writes++ }, { writes++ }, { retries++ }, {})
            }
        }
        compose.onNodeWithTag("language_ENGLISH").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("theme_DARK").performScrollTo().assertIsNotEnabled()
        assertEquals(0, writes)
        // Retry remains visible in the error card at the beginning of the scroll container.
        compose.onNodeWithTag("settings_screen").performTouchInput { swipeDown() }
        compose.onNodeWithText(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(com.ngyennhi.expensemanager.R.string.retry)).performScrollTo().performClick()
        assertEquals(1, retries)
    }
}
