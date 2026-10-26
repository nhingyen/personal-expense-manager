package com.ngyennhi.expensemanager.domain.repository

import com.ngyennhi.expensemanager.domain.model.AppLanguage
import com.ngyennhi.expensemanager.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
    fun getLanguage(): AppLanguage
    suspend fun setLanguage(language: AppLanguage)
}
