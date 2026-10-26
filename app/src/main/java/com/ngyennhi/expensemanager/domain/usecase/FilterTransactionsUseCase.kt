package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Transaction
import com.ngyennhi.expensemanager.domain.model.TransactionDateRange
import com.ngyennhi.expensemanager.domain.model.TransactionType
import java.util.Locale

/** Filters one observed snapshot, preserving the repository's date/id ordering. */
class FilterTransactionsUseCase {
    operator fun invoke(
        transactions: List<Transaction>,
        period: TransactionDateRange,
        query: String = "",
        type: TransactionType? = null,
        category: String? = null,
    ): List<Transaction> {
        val search = query.trim().lowercase(Locale.ROOT)
        return transactions.filter { record ->
            record.date in period &&
                (type == null || record.type == type) &&
                (category == null || record.category.trim().equals(category.trim(), ignoreCase = true)) &&
                (search.isEmpty() || record.category.lowercase(Locale.ROOT).contains(search) ||
                    record.note.orEmpty().lowercase(Locale.ROOT).contains(search))
        }
    }
}
