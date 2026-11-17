package com.ngyennhi.expensemanager.di

import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.repository.RoomCategoryRepository
import com.ngyennhi.expensemanager.domain.repository.CategoryRepository
import com.ngyennhi.expensemanager.domain.usecase.CreateCategoryUseCase
import com.ngyennhi.expensemanager.domain.usecase.DeleteCategoryUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveCategoriesUseCase
import com.ngyennhi.expensemanager.domain.usecase.RenameCategoryUseCase
import com.ngyennhi.expensemanager.domain.usecase.ResolveCategoryInputUseCase
import com.ngyennhi.expensemanager.domain.usecase.SetCategoryArchivedUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CategoryRepositoryModule {
    @Binds abstract fun bindCategoryRepository(implementation: RoomCategoryRepository): CategoryRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CategoryModule {
    @Provides fun dao(database: ExpenseManagerDatabase) = database.categoryDao()
    @Provides fun observe(repository: CategoryRepository) = ObserveCategoriesUseCase(repository)
    @Provides fun create(repository: CategoryRepository) = CreateCategoryUseCase(repository)
    @Provides fun rename(repository: CategoryRepository) = RenameCategoryUseCase(repository)
    @Provides fun archive(repository: CategoryRepository) = SetCategoryArchivedUseCase(repository)
    @Provides fun delete(repository: CategoryRepository) = DeleteCategoryUseCase(repository)
    @Provides fun resolve(repository: CategoryRepository) = ResolveCategoryInputUseCase(repository)
}
