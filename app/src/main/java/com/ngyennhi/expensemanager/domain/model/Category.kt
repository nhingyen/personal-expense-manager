package com.ngyennhi.expensemanager.domain.model

import java.util.Locale

const val MAX_CATEGORY_NAME_LENGTH = 60

fun String.categoryNameLength(): Int = codePointCount(0, length)

fun String.normalizedCategoryName(): String = trim().lowercase(Locale.ROOT)

data class Category(
    val id: Long,
    val type: TransactionType,
    val name: String,
    val isArchived: Boolean,
)
