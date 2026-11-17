package com.ngyennhi.expensemanager.presentation.category

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus

enum class CategoryNameError { REQUIRED, TOO_LONG, DUPLICATE }
enum class CategoryActionError { READ_FAILED, WRITE_FAILED, DELETE_IN_USE, UNAVAILABLE }

data class CategoryEditorState(
    val id: Long? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val name: String = "",
    val nameError: CategoryNameError? = null,
)

data class CategoryUiState(
    val status: LoadStatus = LoadStatus.LOADING,
    val categories: List<Category> = emptyList(),
    val editor: CategoryEditorState? = null,
    val writing: Boolean = false,
    val actionError: CategoryActionError? = null,
) {
    fun active(type: TransactionType): List<Category> = categories.filter { it.type == type && !it.isArchived }
    fun archived(type: TransactionType): List<Category> = categories.filter { it.type == type && it.isArchived }
}
