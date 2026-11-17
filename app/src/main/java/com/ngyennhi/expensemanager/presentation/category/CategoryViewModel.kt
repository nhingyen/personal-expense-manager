package com.ngyennhi.expensemanager.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.MAX_CATEGORY_NAME_LENGTH
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.domain.model.categoryNameLength
import com.ngyennhi.expensemanager.domain.repository.CategoryDeleteResult
import com.ngyennhi.expensemanager.domain.repository.CategorySaveResult
import com.ngyennhi.expensemanager.domain.usecase.CreateCategoryUseCase
import com.ngyennhi.expensemanager.domain.usecase.DeleteCategoryUseCase
import com.ngyennhi.expensemanager.domain.usecase.ObserveCategoriesUseCase
import com.ngyennhi.expensemanager.domain.usecase.RenameCategoryUseCase
import com.ngyennhi.expensemanager.domain.usecase.SetCategoryArchivedUseCase
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val observeCategories: ObserveCategoriesUseCase,
    private val createCategory: CreateCategoryUseCase,
    private val renameCategory: RenameCategoryUseCase,
    private val setArchived: SetCategoryArchivedUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = mutableState.asStateFlow()
    private var observationJob: Job? = null

    init { retry() }

    fun retry() {
        observationJob?.cancel()
        mutableState.value = mutableState.value.copy(status = LoadStatus.LOADING,
            actionError = null)
        observationJob = viewModelScope.launch {
            observeCategories().catch { cause ->
                if (cause is CancellationException) throw cause
                mutableState.value = mutableState.value.copy(status = LoadStatus.ERROR,
                    actionError = CategoryActionError.READ_FAILED)
            }.collect { categories ->
                mutableState.value = mutableState.value.copy(status = LoadStatus.READY,
                    categories = categories, actionError = null)
            }
        }
    }

    fun startCreate() = edit { it.copy(editor = CategoryEditorState(), actionError = null) }

    fun startEdit(category: Category) = edit {
        it.copy(editor = CategoryEditorState(category.id, category.type, category.name), actionError = null)
    }

    fun dismissEditor() = edit { it.copy(editor = null, actionError = null) }

    fun setEditorType(type: TransactionType) = edit { state ->
        val editor = state.editor ?: return@edit state
        if (editor.id != null) state else state.copy(editor = editor.copy(type = type, nameError = null))
    }

    fun setEditorName(name: String) = edit { state ->
        state.copy(editor = state.editor?.copy(name = name, nameError = null), actionError = null)
    }

    fun saveEditor() {
        val state = mutableState.value
        val editor = state.editor ?: return
        if (state.writing) return
        val trimmed = editor.name.trim()
        val error = when {
            trimmed.isEmpty() -> CategoryNameError.REQUIRED
            trimmed.categoryNameLength() > MAX_CATEGORY_NAME_LENGTH -> CategoryNameError.TOO_LONG
            else -> null
        }
        if (error != null) {
            mutableState.value = state.copy(editor = editor.copy(nameError = error))
            return
        }
        write {
            val result = if (editor.id == null) createCategory(editor.type, trimmed)
            else renameCategory(editor.id, trimmed)
            when (result) {
                is CategorySaveResult.Saved -> mutableState.value = mutableState.value.copy(editor = null)
                CategorySaveResult.Duplicate -> mutableState.value = mutableState.value.copy(
                    editor = mutableState.value.editor?.copy(nameError = CategoryNameError.DUPLICATE))
                CategorySaveResult.NotFound -> mutableState.value = mutableState.value.copy(
                    editor = null, actionError = CategoryActionError.UNAVAILABLE)
            }
        }
    }

    fun toggleArchived(category: Category) = write {
        if (!setArchived(category.id, !category.isArchived)) {
            mutableState.value = mutableState.value.copy(editor = null,
                actionError = CategoryActionError.UNAVAILABLE)
        } else mutableState.value = mutableState.value.copy(editor = null)
    }

    fun delete(category: Category) = write {
        when (deleteCategory(category.id)) {
            CategoryDeleteResult.DELETED -> mutableState.value = mutableState.value.copy(editor = null)
            CategoryDeleteResult.IN_USE -> mutableState.value = mutableState.value.copy(
                actionError = CategoryActionError.DELETE_IN_USE)
            CategoryDeleteResult.NOT_FOUND -> mutableState.value = mutableState.value.copy(
                editor = null, actionError = CategoryActionError.UNAVAILABLE)
        }
    }

    fun clearError() { mutableState.value = mutableState.value.copy(actionError = null) }

    private fun write(block: suspend () -> Unit) {
        if (mutableState.value.writing) return
        mutableState.value = mutableState.value.copy(writing = true, actionError = null)
        viewModelScope.launch {
            try { block() }
            catch (cause: CancellationException) { throw cause }
            catch (_: Exception) {
                mutableState.value = mutableState.value.copy(actionError = CategoryActionError.WRITE_FAILED)
            } finally { mutableState.value = mutableState.value.copy(writing = false) }
        }
    }

    private fun edit(block: (CategoryUiState) -> CategoryUiState) {
        if (!mutableState.value.writing) mutableState.value = block(mutableState.value)
    }
}
