package com.ngyennhi.expensemanager.domain.repository

import com.ngyennhi.expensemanager.domain.model.Wallet
import com.ngyennhi.expensemanager.domain.model.WalletDraft
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun observeWallets(): Flow<List<Wallet>>
    suspend fun add(wallet: WalletDraft): Long
    suspend fun update(wallet: Wallet): Boolean
    /** Atomically refuses the default wallet and wallets that have transactions. */
    suspend fun deleteEmpty(id: Long): Boolean
}
