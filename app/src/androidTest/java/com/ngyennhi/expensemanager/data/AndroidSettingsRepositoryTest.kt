package com.ngyennhi.expensemanager.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ngyennhi.expensemanager.data.repository.AndroidSettingsRepository
import com.ngyennhi.expensemanager.domain.model.AppTheme
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSettingsRepositoryTest {
    @Test fun themesPersistAcrossRepositoryReopenAndUnknownValueUsesSystem(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("task010_repository_test", Context.MODE_PRIVATE)
        try {
            preferences.edit().clear().commit()
            assertEquals(AppTheme.SYSTEM, AndroidSettingsRepository(preferences).observeTheme().first())
            for (theme in AppTheme.entries) {
                AndroidSettingsRepository(preferences).setTheme(theme)
                val reopened = AndroidSettingsRepository(context.getSharedPreferences("task010_repository_test", Context.MODE_PRIVATE))
                assertEquals(theme, reopened.observeTheme().first())
                val disk = File(context.applicationInfo.dataDir, "shared_prefs/task010_repository_test.xml")
                val entries = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(disk).getElementsByTagName("string")
                assertEquals(theme.name, entries.item(0).textContent)
            }
            preferences.edit().putString("theme", "future_value").commit()
            assertEquals(AppTheme.SYSTEM, AndroidSettingsRepository(preferences).observeTheme().first())
        } finally { preferences.edit().clear().commit() }
    }
}
