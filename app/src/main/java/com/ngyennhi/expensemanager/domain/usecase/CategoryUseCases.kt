package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.MAX_CATEGORY_NAME_LENGTH
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.model.categoryNameLength
import com.ngyennhi.expensemanager.domain.repository.CategoryRepository

class ObserveCategoriesUseCase(private val repository: CategoryRepository) {
    operator fun invoke() = repository.observeCategories()
}
class CreateCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(type: TransactionType, input: String) = validatedName(input).let { name ->
        repository.create(type, name)
    }
}

class RenameCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: Long, input: String) = validatedName(input).let { name ->
        require(id > 0) { "Category ID must be positive" }
        repository.rename(id, name)
    }
}

class SetCategoryArchivedUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: Long, archived: Boolean): Boolean {
        require(id > 0) { "Category ID must be positive" }
        return repository.setArchived(id, archived)
    }
}

class DeleteCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id.also {
        require(it > 0) { "Category ID must be positive" }
    })
}

sealed interface CategoryInputResolution {
    val name: String
    data class New(override val name: String) : CategoryInputResolution
    data class Active(val category: Category) : CategoryInputResolution { override val name = category.name }
    data class Archived(val category: Category) : CategoryInputResolution { override val name = category.name }
}

/** Archived matches are never recreated or restored implicitly. */
class ResolveCategoryInputUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(type: TransactionType, input: String): CategoryInputResolution {
        val name = validatedName(input)
        val match = repository.findByName(type, name)
            ?: return CategoryInputResolution.New(name)
        return if (match.isArchived) CategoryInputResolution.Archived(match)
        else CategoryInputResolution.Active(match)
    }
}

private fun validatedName(input: String): String = input.trim().also { name ->
    require(name.isNotEmpty() && name.categoryNameLength() <= MAX_CATEGORY_NAME_LENGTH) {
        "Category name must contain 1-$MAX_CATEGORY_NAME_LENGTH characters"
    }
}
