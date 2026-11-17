package com.ngyennhi.expensemanager.presentation.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.Category
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.presentation.transaction.CategoryIcon
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.presentation.transaction.LoadStatus
import com.ngyennhi.expensemanager.presentation.transaction.TypeChip
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import com.ngyennhi.expensemanager.ui.theme.appearanceIconFilter

@Composable
internal fun CategoryManagementScreen(
    state: CategoryUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Category) -> Unit,
    onName: (String) -> Unit,
    onType: (TransactionType) -> Unit,
    onSave: () -> Unit,
    onDismissEditor: () -> Unit,
    onToggleArchived: (Category) -> Unit,
    onDelete: (Category) -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = state.writing) { }
    Scaffold(containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(R.string.categories_manage_title),
            onBack = { if (!state.writing) onBack() }, action = {
                IconButton(onClick = onAdd, enabled = state.status == LoadStatus.READY && !state.writing,
                    modifier = Modifier.testTag("category_add_top")) {
                    Image(painterResource(R.drawable.ic_plus), stringResource(R.string.category_add),
                        Modifier.size(22.dp), colorFilter = appearanceIconFilter(ExpenseManagerColors.primary))
                }
            }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("category_management"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(stringResource(R.string.categories_manage_hint),
                style = MaterialTheme.typography.bodyMedium, color = ExpenseManagerColors.textSecondary) }
            when (state.status) {
                LoadStatus.LOADING -> item { CircularProgressIndicator() }
                LoadStatus.ERROR -> item { ErrorBanner(stringResource(R.string.category_read_error), onRetry) }
                LoadStatus.READY -> {
                    for (type in listOf(TransactionType.EXPENSE, TransactionType.INCOME)) {
                        val active = state.active(type)
                        val archived = state.archived(type)
                        item(key = "heading_${type.name}") {
                            Text(stringResource(if (type == TransactionType.EXPENSE)
                                R.string.category_expense_heading else R.string.category_income_heading),
                                style = MaterialTheme.typography.titleMedium)
                        }
                        if (active.isEmpty()) item(key = "empty_${type.name}") {
                            Text(stringResource(R.string.category_empty), style = MaterialTheme.typography.bodySmall,
                                color = ExpenseManagerColors.textSecondary)
                        }
                        active.chunked(2).forEachIndexed { index, row -> item(key = "${type.name}_$index") {
                            CategoryCardRow(row, onEdit)
                        } }
                        if (archived.isNotEmpty()) {
                            item(key = "archived_heading_${type.name}") {
                                Text(stringResource(R.string.category_archived_heading),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ExpenseManagerColors.textSecondary)
                            }
                            archived.chunked(2).forEachIndexed { index, row -> item(key = "archived_${type.name}_$index") {
                                CategoryCardRow(row, onEdit)
                            } }
                        }
                    }
                    item {
                        Button(onClick = onAdd, enabled = !state.writing,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("category_add_button")) {
                            Text(stringResource(R.string.category_add))
                        }
                    }
                }
            }
            state.actionError?.let { error -> item {
                ErrorBanner(stringResource(when (error) {
                    CategoryActionError.READ_FAILED -> R.string.category_read_error
                    CategoryActionError.WRITE_FAILED -> R.string.category_write_error
                    CategoryActionError.DELETE_IN_USE -> R.string.category_delete_in_use
                    CategoryActionError.UNAVAILABLE -> R.string.category_unavailable
                }), if (error == CategoryActionError.READ_FAILED) onRetry else onClearError)
            } }
        }
    }
    state.editor?.let { editor ->
        val category = editor.id?.let { id -> state.categories.firstOrNull { it.id == id } }
        CategoryEditorDialog(editor, category, state.writing, onName, onType, onSave,
            onDismissEditor, onToggleArchived, { pendingDeleteId = it.id })
    }
    pendingDeleteId?.let { id ->
        val category = state.categories.firstOrNull { it.id == id }
        if (category == null) pendingDeleteId = null else AlertDialog(
            onDismissRequest = { if (!state.writing) pendingDeleteId = null },
            title = { Text(stringResource(R.string.category_delete)) },
            text = { Text(stringResource(R.string.category_delete_confirm)) },
            confirmButton = { TextButton(onClick = {
                pendingDeleteId = null
                onDelete(category)
            }, enabled = !state.writing, modifier = Modifier.testTag("category_confirm_delete")) {
                Text(stringResource(R.string.category_delete))
            } },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }, enabled = !state.writing) {
                Text(stringResource(R.string.cancel))
            } },
        )
    }
}

@Composable
private fun CategoryCardRow(categories: List<Category>, onEdit: (Category) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        categories.forEach { category ->
            Surface(onClick = { onEdit(category) }, color = ExpenseManagerColors.surface,
                shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f).heightIn(min = 118.dp)
                    .testTag("category_card_${category.id}")) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CategoryIcon(category.name, category.type, 38.dp)
                    Text(category.name, style = MaterialTheme.typography.labelLarge)
                    if (category.isArchived) Text(stringResource(R.string.category_archived),
                        style = MaterialTheme.typography.bodySmall, color = ExpenseManagerColors.textSecondary)
                }
            }
        }
        repeat(2 - categories.size) { Box(Modifier.weight(1f)) }
    }
}

@Composable
private fun CategoryEditorDialog(
    editor: CategoryEditorState,
    category: Category?,
    writing: Boolean,
    onName: (String) -> Unit,
    onType: (TransactionType) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onToggleArchived: (Category) -> Unit,
    onDelete: (Category) -> Unit,
) {
    AlertDialog(onDismissRequest = { if (!writing) onDismiss() },
        title = { Text(stringResource(if (editor.id == null) R.string.category_add else R.string.category_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editor.id == null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip(stringResource(R.string.type_expense), editor.type == TransactionType.EXPENSE,
                        { onType(TransactionType.EXPENSE) }, Modifier.weight(1f), !writing)
                    TypeChip(stringResource(R.string.type_income), editor.type == TransactionType.INCOME,
                        { onType(TransactionType.INCOME) }, Modifier.weight(1f), !writing)
                }
                OutlinedTextField(editor.name, onName, enabled = !writing, singleLine = true,
                    label = { Text(stringResource(R.string.category_name)) },
                    isError = editor.nameError != null,
                    supportingText = { editor.nameError?.let { Text(stringResource(when (it) {
                        CategoryNameError.REQUIRED -> R.string.field_required
                        CategoryNameError.TOO_LONG -> R.string.category_name_invalid
                        CategoryNameError.DUPLICATE -> R.string.category_duplicate
                    })) } }, modifier = Modifier.fillMaxWidth().testTag("category_editor_name"))
                category?.let {
                    OutlinedButton(onClick = { onToggleArchived(it) }, enabled = !writing,
                        modifier = Modifier.fillMaxWidth().testTag("category_toggle_archive")) {
                        Text(stringResource(if (it.isArchived) R.string.category_restore else R.string.category_archive))
                    }
                    TextButton(onClick = { onDelete(it) }, enabled = !writing,
                        modifier = Modifier.fillMaxWidth().testTag("category_delete")) {
                        Text(stringResource(R.string.category_delete), color = ExpenseManagerColors.expense)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onSave, enabled = !writing,
            modifier = Modifier.testTag("category_editor_save")) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !writing) {
            Text(stringResource(R.string.cancel))
        } })
}
