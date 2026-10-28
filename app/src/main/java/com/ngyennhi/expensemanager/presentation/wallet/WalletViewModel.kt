package com.ngyennhi.expensemanager.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.*
import com.ngyennhi.expensemanager.domain.usecase.*
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class WalletError { READ, OVERFLOW, WRITE, DELETE_BLOCKED }
data class WalletFormState(
    val id: Long? = null,
    val name: String = "",
    val type: WalletType = WalletType.CASH,
    val openingInput: String = "0",
    val invalidName: Boolean = false,
    val invalidOpening: Boolean = false,
)
data class WalletUiState(
    val status: LoadStatus = LoadStatus.LOADING,
    val wallets: List<Wallet> = emptyList(),
    val balances: List<WalletBalance> = emptyList(),
    val totalVnd: Long? = null,
    val transactions: List<Transaction> = emptyList(),
    val error: WalletError? = null,
    val form: WalletFormState = WalletFormState(),
    val writing: Boolean = false,
)

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val observeWallets: ObserveWalletsUseCase,
    private val observeTransactions: ObserveTransactionsUseCase,
    private val saveWallet: SaveWalletUseCase,
    private val deleteWallet: DeleteWalletUseCase,
    private val calculate: CalculateWalletBalancesUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WalletUiState())
    val uiState = mutableState.asStateFlow()
    private val navigation = Channel<Long?>(Channel.BUFFERED)
    val navigationEvents = navigation.receiveAsFlow()
    private var observer: Job? = null

    init { retry() }

    fun retry() {
        observer?.cancel()
        mutableState.update { it.copy(status = LoadStatus.LOADING, error = null, balances = emptyList(), totalVnd = null) }
        observer = viewModelScope.launch {
            combine(observeWallets(), observeTransactions()) { wallets, records -> wallets to records }
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    mutableState.update { it.copy(status = LoadStatus.ERROR, error = WalletError.READ,
                        wallets = emptyList(), transactions = emptyList(), balances = emptyList(), totalVnd = null) }
                }.collect { (wallets, records) ->
                    try {
                        val balances = calculate(wallets, records)
                        val total = calculate.total(balances)
                        mutableState.update { it.copy(status = LoadStatus.READY, wallets = wallets, transactions = records,
                            balances = balances, totalVnd = total, error = it.error?.takeIf { e -> e == WalletError.WRITE || e == WalletError.DELETE_BLOCKED }) }
                    } catch (_: ArithmeticException) {
                        mutableState.update { it.copy(status = LoadStatus.READY, wallets = wallets, transactions = records,
                            balances = emptyList(), totalVnd = null, error = WalletError.OVERFLOW) }
                    }
                }
        }
    }

    fun startAdd() {
        if (uiState.value.writing) return
        mutableState.update { it.copy(form = WalletFormState(), error = it.error?.takeIf { e -> e == WalletError.OVERFLOW || e == WalletError.READ }) }
    }
    fun startEdit(id: Long) {
        if (uiState.value.writing) return
        val wallet = uiState.value.wallets.firstOrNull { it.id == id } ?: return
        mutableState.update { it.copy(form = WalletFormState(wallet.id, wallet.name, wallet.type, wallet.openingBalanceVnd.toString()),
            error = it.error?.takeIf { e -> e == WalletError.OVERFLOW || e == WalletError.READ }) }
    }
    fun setName(value: String) = edit { it.copy(name = value, invalidName = false) }
    fun setType(value: WalletType) = edit { it.copy(type = value) }
    fun setOpening(value: String) = edit { it.copy(openingInput = value, invalidOpening = false) }
    private fun edit(block: (WalletFormState) -> WalletFormState) {
        if (!uiState.value.writing) mutableState.update { it.copy(form = block(it.form), error = it.error?.takeUnless { e -> e == WalletError.WRITE }) }
    }

    fun save() {
        val state = uiState.value
        if (state.writing || state.status != LoadStatus.READY) return
        val form = state.form
        val raw = form.openingInput.trim()
        val opening = raw.takeIf { it.matches(Regex("-?[0-9]+")) }?.toLongOrNull()
        val invalidName = form.name.trim().isEmpty() || form.name.trim().length > 60
        if (invalidName || opening == null) {
            mutableState.update { it.copy(form = form.copy(invalidName = invalidName, invalidOpening = opening == null)) }
            return
        }
        write {
            val id = saveWallet(form.id, WalletDraft(form.name, form.type, opening))
            navigation.send(id)
        }
    }
    fun delete(id: Long) {
        if (uiState.value.writing || uiState.value.status != LoadStatus.READY) return
        write {
            if (deleteWallet(id)) navigation.send(null)
            else mutableState.update { it.copy(error = WalletError.DELETE_BLOCKED) }
        }
    }
    private fun write(action: suspend () -> Unit) {
        mutableState.update { it.copy(writing = true, error = null) }
        viewModelScope.launch {
            try { action() }
            catch (cause: CancellationException) { throw cause }
            catch (_: Exception) { mutableState.update { it.copy(error = WalletError.WRITE) } }
            finally { mutableState.update { it.copy(writing = false) } }
        }
    }
}
