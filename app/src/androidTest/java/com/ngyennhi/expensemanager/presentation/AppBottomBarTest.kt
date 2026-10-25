package com.ngyennhi.expensemanager.presentation

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.presentation.common.components.AppBottomBar
import com.ngyennhi.expensemanager.presentation.navigation.AppDestination
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppBottomBarTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun figmaGeometryLocalizedLabelsSelectionAndAddActionWorkWithLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var locale by mutableStateOf("vi")
        var scale by mutableStateOf(1f)
        var selected by mutableStateOf(AppDestination.HOME)
        var adds = 0
        composeRule.setContent {
            val config = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(locale)); fontScale = scale
            }
            CompositionLocalProvider(
                LocalContext provides context.createConfigurationContext(config),
                LocalConfiguration provides config,
                LocalDensity provides Density(LocalDensity.current.density, scale),
            ) {
                PersonalExpenseManagerTheme { AppBottomBar(selected, { selected = it }, { adds++ }) }
            }
        }
        composeRule.onNodeWithTag("bottom_bar_content").assertHeightIsEqualTo(80.dp)
        composeRule.onNodeWithTag("nav_indicator_home", true).assertWidthIsEqualTo(58.dp).assertHeightIsEqualTo(32.dp)
        composeRule.onNodeWithTag("nav_indicator_add", true).assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(32.dp)
        for (language in listOf("vi", "en")) {
            composeRule.runOnIdle { locale = language; scale = 1.3f }
            val resources = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            }).resources
            for (tab in AppDestination.entries) {
                composeRule.onNodeWithTag("nav_${tab.route}").assertHeightIsAtLeast(48.dp)
                    .assertWidthIsAtLeast(48.dp).assertIsDisplayed()
                composeRule.onNodeWithText(resources.getString(tab.label)).assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                composeRule.onNodeWithText(resources.getString(tab.label), useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals("Localized label should fit at 1.3x: $tab", 1, layouts.single().lineCount)
                composeRule.onNodeWithTag("nav_icon_${tab.route}", true)
                    .assertHeightIsEqualTo(22.dp).assertWidthIsEqualTo(22.dp)
                if (tab != AppDestination.ADD) {
                    composeRule.onNodeWithTag("nav_${tab.route}").performClick().assertIsSelected()
                }
            }
            composeRule.onNodeWithTag("nav_add").performClick()
            composeRule.onNodeWithTag("nav_settings").assertIsSelected()
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.waitForIdle()
            composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(context.getExternalFilesDir(null), "task005-bottom-$language.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
        assertEquals(2, adds)
    }
}
