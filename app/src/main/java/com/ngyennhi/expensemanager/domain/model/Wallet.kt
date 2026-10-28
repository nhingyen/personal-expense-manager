package com.ngyennhi.expensemanager.domain.model

const val DEFAULT_WALLET_ID = 1L

enum class WalletType { CASH, BANK, E_WALLET }

data class Wallet(val id: Long, val name: String, val type: WalletType, val openingBalanceVnd: Long)
data class WalletDraft(val name: String, val type: WalletType, val openingBalanceVnd: Long)
data class WalletBalance(val wallet: Wallet, val balanceVnd: Long, val transactions: List<Transaction>)
