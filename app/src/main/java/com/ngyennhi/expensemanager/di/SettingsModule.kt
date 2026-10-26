package com.ngyennhi.expensemanager.di

import android.content.Context
import com.ngyennhi.expensemanager.data.repository.AndroidSettingsRepository
import com.ngyennhi.expensemanager.domain.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides @Singleton
    fun repository(@ApplicationContext context: Context): SettingsRepository =
        AndroidSettingsRepository(context.getSharedPreferences("appearance", Context.MODE_PRIVATE))
}
