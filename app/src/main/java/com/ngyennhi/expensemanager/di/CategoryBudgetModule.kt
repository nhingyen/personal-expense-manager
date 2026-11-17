package com.ngyennhi.expensemanager.di

import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.repository.RoomCategoryBudgetRepository
import com.ngyennhi.expensemanager.domain.repository.CategoryBudgetRepository
import com.ngyennhi.expensemanager.domain.usecase.CalculateCategoryBudgetsUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveCategoryBudgetsUseCase
import com.ngyennhi.expensemanager.domain.usecase.SaveCategoryBudgetUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryBudgetRepositoryModule {
    @Binds
    abstract fun bindCategoryBudgetRepository(
        implementation: RoomCategoryBudgetRepository,
    ): CategoryBudgetRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CategoryBudgetModule {
    @Provides fun dao(database: ExpenseManagerDatabase) = database.categoryBudgetDao()
    @Provides fun observe(repository: CategoryBudgetRepository) = ObserveCategoryBudgetsUseCase(repository)
    @Provides fun save(repository: CategoryBudgetRepository) = SaveCategoryBudgetUseCase(repository)
    @Provides fun calculate() = CalculateCategoryBudgetsUseCase()
}
