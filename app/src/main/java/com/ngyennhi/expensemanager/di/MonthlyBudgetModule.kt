package com.ngyennhi.expensemanager.di

import android.content.Context
import com.ngyennhi.expensemanager.data.repository.PreferencesMonthlyBudgetRepository
import com.ngyennhi.expensemanager.domain.repository.MonthlyBudgetRepository
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlyBudgetUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveMonthlyBudgetsUseCase
import com.ngyennhi.expensemanager.domain.usecase.SaveMonthlyBudgetUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MonthlyBudgetStorageModule {
    @Provides @Singleton
    fun repository(@ApplicationContext context: Context): MonthlyBudgetRepository =
        PreferencesMonthlyBudgetRepository(context.getSharedPreferences("monthly_budgets", Context.MODE_PRIVATE))
}

@Module
@InstallIn(SingletonComponent::class)
object MonthlyBudgetModule {
    @Provides fun observe(repository: MonthlyBudgetRepository) = ObserveMonthlyBudgetsUseCase(repository)
    @Provides fun save(repository: MonthlyBudgetRepository) = SaveMonthlyBudgetUseCase(repository)
    @Provides fun calculate() = CalculateMonthlyBudgetUseCase()
}
