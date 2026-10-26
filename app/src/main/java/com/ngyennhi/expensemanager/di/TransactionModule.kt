package com.ngyennhi.expensemanager.di

import android.content.Context
import androidx.room.Room
import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.local.TransactionDao
import com.ngyennhi.expensemanager.data.repository.RoomTransactionRepository
import com.ngyennhi.expensemanager.domain.repository.TransactionRepository
import com.ngyennhi.expensemanager.domain.usecase.AddTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.CalculateMonthlySummaryUseCase
import com.ngyennhi.expensemanager.domain.usecase.DeleteTransactionUseCase
import com.ngyennhi.expensemanager.domain.usecase.FilterTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveTransactionsUseCase
import com.ngyennhi.expensemanager.domain.usecase.UpdateTransactionUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransactionDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpenseManagerDatabase =
        Room.databaseBuilder(context, ExpenseManagerDatabase::class.java, "expense_manager.db").build()

    @Provides
    fun provideTransactionDao(database: ExpenseManagerDatabase): TransactionDao =
        database.transactionDao()
}

@Module
@InstallIn(SingletonComponent::class)
object TransactionUseCaseModule {
    @Provides
    fun provideFilterTransactionsUseCase(): FilterTransactionsUseCase = FilterTransactionsUseCase()

    @Provides
    fun provideAddTransactionUseCase(repository: TransactionRepository): AddTransactionUseCase =
        AddTransactionUseCase(repository)

    @Provides
    fun provideUpdateTransactionUseCase(repository: TransactionRepository): UpdateTransactionUseCase =
        UpdateTransactionUseCase(repository)

    @Provides
    fun provideDeleteTransactionUseCase(repository: TransactionRepository): DeleteTransactionUseCase =
        DeleteTransactionUseCase(repository)

    @Provides
    fun provideObserveTransactionsUseCase(repository: TransactionRepository): ObserveTransactionsUseCase =
        ObserveTransactionsUseCase(repository)

    @Provides
    fun provideCalculateMonthlySummaryUseCase() : CalculateMonthlySummaryUseCase {
        return CalculateMonthlySummaryUseCase()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TransactionRepositoryModule {
    @Binds
    abstract fun bindTransactionRepository(implementation: RoomTransactionRepository): TransactionRepository
}
