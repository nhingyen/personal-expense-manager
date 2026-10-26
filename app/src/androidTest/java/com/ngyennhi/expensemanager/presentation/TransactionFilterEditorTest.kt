package com.ngyennhi.expensemanager.presentation

import android.app.LocaleManager
import android.graphics.Bitmap
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDateRange
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.transaction.TransactionFilterSheet
import com.ngyennhi.expensemanager.presentation.transaction.TransactionUiState
import com.ngyennhi.expensemanager.ui.theme.PersonalExpenseManagerTheme
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@SdkSuppress(minSdkVersion = 33)
class TransactionFilterEditorTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    // Configure Android before the Activity starts: dialog windows obtain their own
    // resource/density locals. Restore both settings after the Activity is destroyed.
    @get:Rule(order = 0) val configurationRule = object : TestWatcher() {
        private lateinit var originalLocales: LocaleList
        private var originalScale = 1f
        override fun starting(description: Description) {
            val manager = context.getSystemService(LocaleManager::class.java)
            originalLocales = manager.applicationLocales
            originalScale = Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE, 1f)
            manager.applicationLocales = LocaleList.forLanguageTags(
                if (description.methodName.startsWith("vietnamese")) "vi" else "en")
            setFontScale(1.3f)
        }
        override fun finished(description: Description) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = originalLocales
            setFontScale(originalScale)
        }
    }
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun vietnameseEditorSelectsInclusiveRangeAndAppliesAtLargeText() = verifyEditor("vi")
    @Test fun englishEditorSelectsInclusiveRangeAndAppliesAtLargeText() = verifyEditor("en")

    private fun setFontScale(scale: Float) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation
            .executeShellCommand("settings put system font_scale $scale")).use { it.readBytes() }
        instrumentation.waitForIdleSync()
    }

    private fun verifyEditor(language: String) {
        // Locale/font changes are delivered asynchronously and can recreate the Activity.
        composeRule.waitUntil(5_000) {
            val configuration = composeRule.activity.resources.configuration
            configuration.locales[0].language == language && kotlin.math.abs(configuration.fontScale - 1.3f) < 0.01f
        }
        assertEquals(language, composeRule.activity.resources.configuration.locales[0].language)
        assertEquals(1.3f, composeRule.activity.resources.configuration.fontScale, 0.01f)
        var show by mutableStateOf(true)
        var appliedCategory: String? = null
        var appliedRange: TransactionDateRange? = null
        var applyCalls = 0
        val state = TransactionUiState(
            loadStatus = LoadStatus.READY, selectedMonth = YearMonth.of(2026, 10),
            transactions = listOf(Transaction(1, TransactionType.EXPENSE, 100, "Custom", LocalDate.of(2026, 10, 1), null)),
        )
        composeRule.setContent {
            PersonalExpenseManagerTheme {
                if (show) TransactionFilterSheet(state,
                    onDismiss = { show = false }, onReset = { show = false },
                    onApply = { category, range ->
                        appliedCategory = category; appliedRange = range; applyCalls++; show = false
                    })
            }
        }
        composeRule.onNodeWithTag("filter_category").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Custom").performClick()
        composeRule.onNodeWithTag("filter_date_range").performScrollTo().performClick().assertIsSelected()
        composeRule.onNodeWithTag("filter_start_date").performScrollTo().performClick()
        composeRule.onNodeWithTag("cancel_transaction_date").performClick()
        assertNull(appliedRange)

        fun pickDate(tag: String, vi: String, en: String) {
            composeRule.onNodeWithTag(tag).performScrollTo().performClick()
            composeRule.onNode(hasContentDescription(if (language == "vi") "nhập" else "input",
                substring = true, ignoreCase = true)).performClick()
            val input = composeRule.onNode(hasSetTextAction())
            input.performTextClearance()
            composeRule.onNodeWithTag("confirm_transaction_date").assertIsNotEnabled()
            input.performTextInput(if (language == "vi") vi else en)
            Espresso.closeSoftKeyboard()
            composeRule.onNodeWithTag("confirm_transaction_date").assertIsEnabled().performClick()
        }
        pickDate("filter_start_date", "30092026", "09302026")
        pickDate("filter_end_date", "29092026", "09292026")
        composeRule.onNodeWithTag("apply_transaction_filters").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.filter_invalid_range)).assertExists()
        pickDate("filter_end_date", "30092026", "09302026")
        composeRule.onNodeWithTag("apply_transaction_filters").performScrollTo().assertIsEnabled()
        pickDate("filter_end_date", "02102026", "10022026")
        assertNull(appliedRange) // Date confirmation only edits the sheet draft.
        composeRule.onNodeWithTag("transaction_filter_sheet").captureToImage().asAndroidBitmap().let { bitmap ->
            File(context.getExternalFilesDir(null), "task006-filters-$language.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        composeRule.onNodeWithTag("apply_transaction_filters").performScrollTo().performClick()
        assertEquals("Custom", appliedCategory)
        assertEquals(TransactionDateRange(LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 2)), appliedRange)
        assertEquals(1, applyCalls)
    }
}
