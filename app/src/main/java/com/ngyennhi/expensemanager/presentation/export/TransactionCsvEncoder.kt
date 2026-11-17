package com.ngyennhi.expensemanager.presentation.export

import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.wallet.WalletUiState
import java.nio.charset.StandardCharsets

data class CsvExportDocument(
    val bytes: ByteArray,
    val transactionCount: Int,
)

object TransactionCsvEncoder {
    private val headers = listOf(
        "id",
        "type",
        "amount_vnd",
        "category",
        "date",
        "note",
        "wallet_id",
        "wallet_name",
        "wallet_type",
    )

    fun encode(snapshot: WalletUiState): CsvExportDocument {
        require(snapshot.status == LoadStatus.READY) { "Wallet snapshot is not ready" }
        val walletsById = snapshot.wallets.associateBy { it.id }
        require(walletsById.size == snapshot.wallets.size) { "Wallet snapshot contains duplicate IDs" }
        val csv = buildString {
            append('\uFEFF')
            appendRow(headers)
            snapshot.transactions.forEach { transaction ->
                val wallet = requireNotNull(walletsById[transaction.walletId]) {
                    "Transaction ${transaction.id} references a missing wallet"
                }
                appendRow(listOf(
                    transaction.id.toString(),
                    transaction.type.name,
                    transaction.amountVnd.toString(),
                    protectSpreadsheetFormula(transaction.category),
                    transaction.date.toString(),
                    protectSpreadsheetFormula(transaction.note.orEmpty()),
                    transaction.walletId.toString(),
                    protectSpreadsheetFormula(wallet.name),
                    wallet.type.name,
                ))
            }
        }
        return CsvExportDocument(csv.toByteArray(StandardCharsets.UTF_8), snapshot.transactions.size)
    }

    private fun StringBuilder.appendRow(values: List<String>) {
        append(values.joinToString(",", transform = ::escapeCsvCell))
        append("\r\n")
    }

    private fun escapeCsvCell(value: String): String {
        if (value.none { it == ',' || it == '"' || it == '\r' || it == '\n' }) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun protectSpreadsheetFormula(value: String): String {
        val first = value.firstOrNull() ?: return value
        val firstMeaningful = value.firstOrNull { !it.isWhitespace() }
        return if (first == '\t' || first == '\r' || first == '\n' || firstMeaningful == '=' ||
            firstMeaningful == '+' || firstMeaningful == '-' || firstMeaningful == '@') {
            "'$value"
        } else {
            value
        }
    }
}
