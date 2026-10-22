package com.ngyennhi.expensemanager.presentation.transaction

import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.TransactionType

internal fun suggestedCategories(type: TransactionType): List<Int> = when (type) {
    TransactionType.EXPENSE -> listOf(R.string.category_food, R.string.category_coffee,
        R.string.category_shopping, R.string.category_transport)
    TransactionType.INCOME -> listOf(R.string.category_salary)
}
