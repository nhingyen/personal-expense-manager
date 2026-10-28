package com.ngyennhi.expensemanager.di

import com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase
import com.ngyennhi.expensemanager.data.repository.RoomWalletRepository
import com.ngyennhi.expensemanager.domain.repository.WalletRepository
import com.ngyennhi.expensemanager.domain.usecase.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WalletRepositoryModule {
    @Binds abstract fun bindWalletRepository(implementation: RoomWalletRepository): WalletRepository
}

@Module
@InstallIn(SingletonComponent::class)
object WalletModule {
    @Provides fun dao(database: ExpenseManagerDatabase) = database.walletDao()
    @Provides fun observe(repository: WalletRepository) = ObserveWalletsUseCase(repository)
    @Provides fun save(repository: WalletRepository) = SaveWalletUseCase(repository)
    @Provides fun delete(repository: WalletRepository) = DeleteWalletUseCase(repository)
    @Provides fun balances() = CalculateWalletBalancesUseCase()
}
