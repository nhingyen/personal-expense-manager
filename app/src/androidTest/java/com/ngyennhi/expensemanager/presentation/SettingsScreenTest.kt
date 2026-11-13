package com.ngyennhi.expensemanager.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import com.ngyennhi.expensemanager.presentation.settings.SettingsScreen
import com.ngyennhi.expensemanager.presentation.settings.SettingsUiState
import com.ngyennhi.expensemanager.presentation.settings.CsvExportStatus
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
        var categories = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                PersonalExpenseManagerTheme(darkTheme = true) {
                    SettingsScreen(SettingsUiState(isLoading = false), { language = it }, { theme = it }, {}, {}, {},
                        { categories++ })
                }
            }
        }
        compose.onNodeWithTag("language_ENGLISH").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(AppLanguage.ENGLISH, language)
        compose.onNodeWithTag("theme_DARK").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(AppTheme.DARK, theme)
        compose.onNodeWithTag("settings_version").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings_export_csv").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings_categories").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, categories)
    }

    @Test fun readFailureBlocksWritesAndExposesRetry() {
        var retries = 0
        var writes = 0
        compose.setContent {
            PersonalExpenseManagerTheme(darkTheme = false) {
                SettingsScreen(SettingsUiState(isLoading = false, readError = true),
                    { writes++ }, { writes++ }, { retries++ }, {}, {})
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

    @Test fun exportIsReachableIndependentFromPreferenceErrorsAndBusyStateBlocksDuplicates() {
        var exports = 0
        var state by androidx.compose.runtime.mutableStateOf(
            SettingsUiState(isLoading = false, readError = true),
        )
        compose.setContent {
            PersonalExpenseManagerTheme(darkTheme = false) {
                SettingsScreen(state, {}, {}, {}, {}, { exports++ })
            }
        }

        compose.onNodeWithTag("settings_export_csv").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1, exports)
        compose.runOnIdle { state = state.copy(exportStatus = CsvExportStatus.WRITING) }
        compose.onNodeWithTag("settings_export_csv").performScrollTo().assertIsNotEnabled()
        assertEquals(1, exports)
        compose.runOnIdle { state = state.copy(exportStatus = CsvExportStatus.PARTIAL_FILE_MAY_REMAIN) }
        compose.onNodeWithTag("settings_export_message").performScrollTo().assertIsDisplayed()
    }
}
