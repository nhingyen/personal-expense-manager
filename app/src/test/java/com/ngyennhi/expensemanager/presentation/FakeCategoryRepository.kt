package com.ngyennhi.expensemanager.presentation

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.CategoryDeleteResult
import com.ngyennhi.expensemanager.domain.repository.CategoryRepository
import com.ngyennhi.expensemanager.domain.repository.CategorySaveResult
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCategoryRepository : CategoryRepository {
    val categories = MutableStateFlow<List<Category>>(emptyList())

    override fun observeCategories() = categories
    override suspend fun findByName(type: TransactionType, name: String): Category? =
        categories.value.firstOrNull { it.type == type && it.name == name }
    override suspend fun create(type: TransactionType, name: String): CategorySaveResult = error("Not used")
    override suspend fun rename(id: Long, name: String): CategorySaveResult = error("Not used")
    override suspend fun setArchived(id: Long, archived: Boolean): Boolean = error("Not used")
    override suspend fun delete(id: Long): CategoryDeleteResult = error("Not used")
}
