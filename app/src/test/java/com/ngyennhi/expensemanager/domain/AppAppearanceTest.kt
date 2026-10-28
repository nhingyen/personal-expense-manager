package com.ngyennhi.expensemanager.domain

import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import org.junit.Assert.*
import org.junit.Test

class AppAppearanceTest {
    @Test fun systemFollowsDeviceAndExplicitThemesIgnoreDevice() {
        for (systemDark in listOf(false, true)) {
            assertEquals(systemDark, AppTheme.SYSTEM.isDark(systemDark))
            assertFalse(AppTheme.LIGHT.isDark(systemDark))
            assertTrue(AppTheme.DARK.isDark(systemDark))
        }
    }

    @Test fun missingOrUnknownPreferencesUseSystem() {
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStored(null))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromStored("future_value"))
        AppTheme.entries.forEach { assertEquals(it, AppTheme.fromStored(it.name)) }
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("fr"))
    }
}
