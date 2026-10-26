package com.ngyennhi.expensemanager.data.repository

import com.ngyennhi.expensemanager.data.local.WalletDao
import com.ngyennhi.expensemanager.data.local.WalletEntity
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.WalletRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomWalletRepository @Inject constructor(private val dao: WalletDao) : WalletRepository {
    override fun observeWallets() = dao.observeAll().map { rows -> rows.map {
        Wallet(it.id, it.name, WalletType.valueOf(it.type), it.openingBalanceVnd)
    } }
    override suspend fun add(wallet: WalletDraft) = dao.insert(
        WalletEntity(name = wallet.name, type = wallet.type.name, openingBalanceVnd = wallet.openingBalanceVnd))
    override suspend fun update(wallet: Wallet) = dao.update(
        WalletEntity(wallet.id, wallet.name, wallet.type.name, wallet.openingBalanceVnd)) > 0
    override suspend fun deleteEmpty(id: Long) = dao.deleteEmpty(id) > 0
}
