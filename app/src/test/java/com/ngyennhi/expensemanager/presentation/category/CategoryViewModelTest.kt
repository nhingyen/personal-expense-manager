package com.ngyennhi.expensemanager.presentation.category

import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.model.normalizedCategoryName
import com.ngyennhi.expensemanager.domain.repository.CategoryDeleteResult
import com.ngyennhi.expensemanager.domain.repository.CategoryRepository
import com.ngyennhi.expensemanager.domain.repository.CategorySaveResult
import com.ngyennhi.expensemanager.domain.usecase.*
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {
    private val repository = FakeCategories()
    private fun viewModel() = CategoryViewModel(ObserveCategoriesUseCase(repository),
        CreateCategoryUseCase(repository), RenameCategoryUseCase(repository),
        SetCategoryArchivedUseCase(repository), DeleteCategoryUseCase(repository))

    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun createRenameAndDuplicateKeepEditorState() = runTest {
        val vm = viewModel(); runCurrent()
        vm.startCreate(); vm.setEditorName("  Travel  "); vm.saveEditor(); runCurrent()
        assertEquals("Travel", repository.rows.value.single().name); assertNull(vm.uiState.value.editor)
        vm.startEdit(repository.rows.value.single()); vm.setEditorName(" Trip "); vm.saveEditor(); runCurrent()
        assertEquals("Trip", repository.rows.value.single().name)
        repository.rows.value += Category(2, TransactionType.EXPENSE, "Food", false)
        vm.startEdit(repository.rows.value.first()); vm.setEditorName(" food "); vm.saveEditor(); runCurrent()
        assertEquals(CategoryNameError.DUPLICATE, vm.uiState.value.editor?.nameError)
        assertEquals(" food ", vm.uiState.value.editor?.name)
    }

    @Test fun archiveRestoreAndGuardedDeleteAreExplicit() = runTest {
        repository.rows.value = listOf(Category(1, TransactionType.EXPENSE, "Food", false)); repository.inUse = true
        val vm = viewModel(); runCurrent()
        vm.toggleArchived(repository.rows.value.single()); runCurrent(); assertTrue(repository.rows.value.single().isArchived)
        vm.toggleArchived(repository.rows.value.single()); runCurrent(); assertFalse(repository.rows.value.single().isArchived)
        vm.delete(repository.rows.value.single()); runCurrent()
        assertEquals(CategoryActionError.DELETE_IN_USE, vm.uiState.value.actionError)
        assertEquals(1, repository.rows.value.size)
    }

    @Test fun validationAndWriteFailureRetainDraft() = runTest {
        val vm = viewModel(); runCurrent(); vm.startCreate(); vm.saveEditor()
        assertEquals(CategoryNameError.REQUIRED, vm.uiState.value.editor?.nameError)
        vm.setEditorName("x".repeat(61)); vm.saveEditor()
        assertEquals(CategoryNameError.TOO_LONG, vm.uiState.value.editor?.nameError)
        vm.setEditorName("Stable"); repository.failWrite = true; vm.saveEditor(); runCurrent()
        assertEquals(CategoryActionError.WRITE_FAILED, vm.uiState.value.actionError)
        assertEquals("Stable", vm.uiState.value.editor?.name); assertFalse(vm.uiState.value.writing)
    }

    @Test fun readFailureAndRetryRecover() = runTest {
        repository.failRead = true
        val vm = viewModel(); runCurrent(); assertEquals(LoadStatus.ERROR, vm.uiState.value.status)
        repository.failRead = false; vm.retry(); runCurrent(); assertEquals(LoadStatus.READY, vm.uiState.value.status)
    }

    private class FakeCategories : CategoryRepository {
        val rows = MutableStateFlow(emptyList<Category>())
        var failRead = false; var failWrite = false; var inUse = false
        override fun observeCategories(): Flow<List<Category>> = if (failRead) flow { error("read") } else rows
        override suspend fun findByName(type: TransactionType, name: String) = rows.value.firstOrNull {
            it.type == type && it.name.normalizedCategoryName() == name.normalizedCategoryName() }
        override suspend fun create(type: TransactionType, name: String): CategorySaveResult {
            if (failWrite) error("write")
            if (findByName(type, name) != null) return CategorySaveResult.Duplicate
            val id = (rows.value.maxOfOrNull { it.id } ?: 0) + 1
            rows.value += Category(id, type, name, false); return CategorySaveResult.Saved(id)
        }
        override suspend fun rename(id: Long, name: String): CategorySaveResult {
            if (failWrite) error("write")
            val row = rows.value.firstOrNull { it.id == id } ?: return CategorySaveResult.NotFound
            if (rows.value.any { it.id != id && it.type == row.type &&
                    it.name.normalizedCategoryName() == name.normalizedCategoryName() }) return CategorySaveResult.Duplicate
            rows.value = rows.value.map { if (it.id == id) it.copy(name = name) else it }
            return CategorySaveResult.Saved(id)
        }
        override suspend fun setArchived(id: Long, archived: Boolean): Boolean {
            if (failWrite) error("write")
            if (rows.value.none { it.id == id }) return false
            rows.value = rows.value.map { if (it.id == id) it.copy(isArchived = archived) else it }; return true
        }
        override suspend fun delete(id: Long): CategoryDeleteResult {
            if (failWrite) error("write")
            if (inUse) return CategoryDeleteResult.IN_USE
            if (rows.value.none { it.id == id }) return CategoryDeleteResult.NOT_FOUND
            rows.value = rows.value.filterNot { it.id == id }; return CategoryDeleteResult.DELETED
        }
    }
}
