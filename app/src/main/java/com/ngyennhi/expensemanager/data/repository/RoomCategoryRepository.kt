package com.ngyennhi.expensemanager.data.repository

import com.ngyennhi.expensemanager.data.local.CategoryDao
import com.ngyennhi.expensemanager.data.local.CategoryEntity
import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.model.normalizedCategoryName
import com.ngyennhi.expensemanager.domain.repository.CategoryDeleteResult
import com.ngyennhi.expensemanager.domain.repository.CategoryRepository
import com.ngyennhi.expensemanager.domain.repository.CategorySaveResult
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class RoomCategoryRepository @Inject constructor(private val dao: CategoryDao) : CategoryRepository {
    override fun observeCategories() = dao.observeAll().map { rows -> rows.map(CategoryEntity::toDomain) }

    override suspend fun findByName(type: TransactionType, name: String) =
        dao.find(type.name, name.normalizedCategoryName())?.toDomain()

    override suspend fun create(type: TransactionType, name: String) =
        dao.createIfAbsent(CategoryEntity(type = type.name, name = name, normalizedName = name.normalizedCategoryName()))
            .let { if (it == CategoryDao.CREATE_DUPLICATE) CategorySaveResult.Duplicate else CategorySaveResult.Saved(it) }

    override suspend fun rename(id: Long, name: String) =
        when (val result = dao.renameAndUpdateTransactions(id, name, name.normalizedCategoryName())) {
            CategoryDao.CREATE_DUPLICATE -> CategorySaveResult.Duplicate
            CategoryDao.NOT_FOUND -> CategorySaveResult.NotFound
            else -> CategorySaveResult.Saved(result)
        }

    override suspend fun setArchived(id: Long, archived: Boolean) = dao.updateArchived(id, archived) > 0

    override suspend fun delete(id: Long) = when (dao.deleteIfUnused(id)) {
        CategoryDao.DELETE_DELETED -> CategoryDeleteResult.DELETED
        CategoryDao.DELETE_IN_USE -> CategoryDeleteResult.IN_USE
        else -> CategoryDeleteResult.NOT_FOUND
    }
}

private fun CategoryEntity.toDomain() = Category(
    id = id,
    type = TransactionType.valueOf(type),
    name = name,
    isArchived = isArchived,
)
