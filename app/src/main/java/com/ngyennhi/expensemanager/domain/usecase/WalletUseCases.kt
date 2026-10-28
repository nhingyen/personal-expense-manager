package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.repository.WalletRepository
import java.math.BigInteger

class ObserveWalletsUseCase(private val repository: WalletRepository) {
    operator fun invoke() = repository.observeWallets()
}

class SaveWalletUseCase(private val repository: WalletRepository) {
    suspend operator fun invoke(id: Long?, draft: WalletDraft): Long {
        val name = draft.name.trim()
        require(name.isNotEmpty() && name.length <= 60) { "Wallet name must contain 1-60 characters" }
        if (id == null) return repository.add(draft.copy(name = name))
        require(id > 0)
        check(repository.update(Wallet(id, name, draft.type, draft.openingBalanceVnd))) { "Wallet unavailable" }
        return id
    }
}

class DeleteWalletUseCase(private val repository: WalletRepository) {
    suspend operator fun invoke(id: Long): Boolean = id != DEFAULT_WALLET_ID && repository.deleteEmpty(id)
}

class CalculateWalletBalancesUseCase {
    operator fun invoke(wallets: List<Wallet>, transactions: List<Transaction>): List<WalletBalance> {
        val grouped = transactions.groupBy { it.walletId }
        return wallets.map { wallet ->
            val records = grouped[wallet.id].orEmpty()
            val balance = records.fold(BigInteger.valueOf(wallet.openingBalanceVnd)) { sum, record ->
                val amount = BigInteger.valueOf(record.amountVnd)
                if (record.type == TransactionType.INCOME) sum + amount else sum - amount
            }.longValueExact()
            WalletBalance(wallet, balance, records)
        }
    }
    fun total(balances: List<WalletBalance>): Long = balances.fold(BigInteger.ZERO) { sum, item ->
        sum + BigInteger.valueOf(item.balanceVnd)
    }.longValueExact()
}
