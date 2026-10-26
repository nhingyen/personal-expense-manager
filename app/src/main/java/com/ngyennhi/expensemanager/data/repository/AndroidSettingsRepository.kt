package com.ngyennhi.expensemanager.data.repository

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import com.ngyennhi.expensemanager.domain.repository.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** AppCompat/framework owns locale persistence; preferences own only appearance. */
class AndroidSettingsRepository(private val preferences: SharedPreferences) : SettingsRepository {
    override fun observeTheme() = callbackFlow {
        fun publish() {
            try {
                trySend(AppTheme.fromStored(preferences.getString(THEME, null)))
            } catch (cause: Exception) { close(cause) }
        }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == THEME) publish()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        publish()
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    override suspend fun setTheme(theme: AppTheme) = withContext(Dispatchers.IO) {
        val editor = preferences.edit()
        editor.putString(THEME, theme.name)
        if (!editor.commit()) {
            throw IOException("Appearance could not be persisted")
        }
    }

    override fun getLanguage(): AppLanguage =
        AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales()[0]?.language.orEmpty())

    override suspend fun setLanguage(language: AppLanguage) = withContext(Dispatchers.Main.immediate) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    private companion object { const val THEME = "theme" }
}
