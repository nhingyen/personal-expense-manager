package com.ngyennhi.expensemanager.domain.usecase

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.repository.CategoryDeleteResult
import com.ngyennhi.expensemanager.domain.repository.CategoryRepository
import com.ngyennhi.expensemanager.domain.repository.CategorySaveResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CategoryUseCasesTest {
    private val repository = FakeCategoryRepository()

    @Test fun createAndRenameTrimAndUseLocaleRootNormalization() = runTest {
        val created = CreateCategoryUseCase(repository)(TransactionType.EXPENSE, "  İTEM  ")
        assertEquals(CategorySaveResult.Saved(1), created)
        assertEquals("İTEM", repository.lastName)
        assertEquals("i̇tem", repository.lastNormalizedName)

        RenameCategoryUseCase(repository)(1, "  Dining  ")
        assertEquals("Dining", repository.lastName)
        assertEquals("dining", repository.lastNormalizedName)
    }

    @Test fun namesUseUnicodeCodePointLimit() = runTest {
        val emoji = "😀"
        CreateCategoryUseCase(repository)(TransactionType.INCOME, emoji.repeat(60))
        for (invalid in listOf("   ", emoji.repeat(61))) {
            try {
                CreateCategoryUseCase(repository)(TransactionType.INCOME, invalid)
                fail("Expected invalid category name")
            } catch (_: IllegalArgumentException) {
                // Expected.
            }
        }
    }

    @Test fun archivedFreeTextIsExplicitAndNeverRecreatedOrRestored() = runTest {
        repository.match = Category(7, TransactionType.EXPENSE, "Food", isArchived = true)
        val result = ResolveCategoryInputUseCase(repository)(TransactionType.EXPENSE, " food ")
        assertTrue(result is CategoryInputResolution.Archived)
        assertEquals("food", repository.lastNormalizedName)
        assertEquals(0, repository.createCalls)
        assertTrue(repository.match!!.isArchived)
    }

    @Test fun freeTextDistinguishesActiveAndNewNames() = runTest {
        repository.match = Category(2, TransactionType.INCOME, "Salary", isArchived = false)
        assertTrue(ResolveCategoryInputUseCase(repository)(TransactionType.INCOME, "SALARY") is CategoryInputResolution.Active)
        repository.match = null
        val result = ResolveCategoryInputUseCase(repository)(TransactionType.EXPENSE, "  Travel  ")
        assertEquals(CategoryInputResolution.New("Travel"), result)
    }

    private class FakeCategoryRepository : CategoryRepository {
        val rows = MutableStateFlow(emptyList<Category>())
        var match: Category? = null
        var lastName = ""
        var lastNormalizedName = ""
        var createCalls = 0

        override fun observeCategories() = rows
        override suspend fun findByName(type: TransactionType, name: String): Category? {
            lastNormalizedName = name.trim().lowercase(java.util.Locale.ROOT)
            return match
        }
        override suspend fun create(type: TransactionType, name: String): CategorySaveResult {
            createCalls++
            lastName = name
            lastNormalizedName = name.lowercase(java.util.Locale.ROOT)
            return CategorySaveResult.Saved(1)
        }
        override suspend fun rename(id: Long, name: String): CategorySaveResult {
            lastName = name
            lastNormalizedName = name.lowercase(java.util.Locale.ROOT)
            return CategorySaveResult.Saved(id)
        }
        override suspend fun setArchived(id: Long, archived: Boolean) = true
        override suspend fun delete(id: Long) = CategoryDeleteResult.DELETED
    }
}
