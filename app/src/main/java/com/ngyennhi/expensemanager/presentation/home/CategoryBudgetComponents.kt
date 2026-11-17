package com.ngyennhi.expensemanager.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.CategoryBudget
import com.ngyennhi.expensemanager.presentation.transaction.CategoryIcon
import com.ngyennhi.expensemanager.presentation.transaction.ErrorBanner
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun CategoryBudgetSection(state: HomeUiState, locale: Locale, actions: HomeActions) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("category_budget_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AdaptiveCategoryBudgetHeading(
                text = stringResource(R.string.category_budgets_title),
                modifier = Modifier.weight(1f),
            )
            val configuredIds = state.categoryBudgetItems.mapTo(mutableSetOf()) { it.category.id }
            val canAdd = state.categoryBudgetLoadStatus == HomeLoadStatus.READY &&
                state.activeExpenseCategories.any { it.id !in configuredIds } && !state.isWritingAnyBudget
            TextButton(
                onClick = actions.onAddCategoryBudget,
                enabled = canAdd,
                modifier = Modifier.heightIn(min = 48.dp).testTag("category_budget_add"),
            ) { Text(stringResource(R.string.category_budget_add)) }
        }

        when (state.categoryBudgetLoadStatus) {
            HomeLoadStatus.LOADING -> CircularProgressIndicator(Modifier.testTag("category_budget_loading"))
            HomeLoadStatus.ERROR -> {
                state.categoryBudgetError?.let {
                    ErrorBanner(stringResource(categoryBudgetErrorMessage(it)), actions.onRetryCategoryBudgets)
                }
                if (state.categoryBudgetStale) {
                    Text(
                        stringResource(R.string.category_budget_stale),
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseManagerColors.textSecondary,
                    )
                }
            }
            HomeLoadStatus.READY -> Unit
        }

        if (state.categoryBudgetItems.isEmpty() && state.categoryBudgetLoadStatus == HomeLoadStatus.READY) {
            Text(
                stringResource(R.string.category_budget_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = ExpenseManagerColors.textSecondary,
                modifier = Modifier.testTag("category_budget_empty"),
            )
        }

        state.categoryBudgetItems.forEach { item ->
            CategoryBudgetCard(item, locale, state, actions)
        }

        state.categoryBudgetEditor?.let { editor ->
            CategoryBudgetEditor(editor, state, actions)
        }

        if (state.categoryBudgetWriteFailed && state.categoryBudgetEditor == null) {
            ErrorBanner(stringResource(R.string.category_budget_save_error))
        }
    }
}

@Composable
private fun CategoryBudgetCard(
    item: CategoryBudgetItem,
    locale: Locale,
    state: HomeUiState,
    actions: HomeActions,
) {
    val budget = item.budget
    Surface(
        color = ExpenseManagerColors.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("category_budget_${item.category.id}"),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CategoryIcon(item.category.name, item.category.type, 34.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.category.name, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        Text(
                            categoryBudgetPercent(budget, locale),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (budget.limitReached) ExpenseManagerColors.expense else ExpenseManagerColors.primary,
                            modifier = Modifier.testTag("category_budget_percent_${item.category.id}"),
                        )
                    }
                    Text(
                        stringResource(
                            R.string.category_budget_spent_limit,
                            homeVnd(budget.spentVnd, locale),
                            homeVnd(budget.limitVnd, locale),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ExpenseManagerColors.textSecondary,
                    )
                    if (item.category.isArchived) {
                        Text(
                            stringResource(R.string.category_archived),
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseManagerColors.textSecondary,
                            modifier = Modifier.testTag("category_budget_archived_${item.category.id}"),
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { (budget.spentVnd.toDouble() / budget.limitVnd).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(7.dp).testTag("category_budget_progress_${item.category.id}"),
                color = if (budget.limitReached) ExpenseManagerColors.expense else ExpenseManagerColors.primary,
                trackColor = ExpenseManagerColors.mutedSurface,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                when {
                    budget.remainingVnd < 0 -> stringResource(
                        R.string.category_budget_exceeded,
                        homeVnd(-budget.remainingVnd, locale),
                    )
                    budget.limitReached -> stringResource(R.string.category_budget_reached)
                    else -> stringResource(R.string.category_budget_remaining, homeVnd(budget.remainingVnd, locale))
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (budget.limitReached) ExpenseManagerColors.expense else ExpenseManagerColors.textSecondary,
                modifier = Modifier.testTag("category_budget_warning_${item.category.id}")
                    .semantics { if (budget.limitReached) liveRegion = LiveRegionMode.Polite },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!item.category.isArchived) {
                    TextButton(
                        onClick = { actions.onEditCategoryBudget(item.category.id) },
                        enabled = !state.isWritingAnyBudget,
                        modifier = Modifier.testTag("category_budget_edit_${item.category.id}"),
                    ) { Text(stringResource(R.string.category_budget_edit)) }
                }
                TextButton(
                    onClick = { actions.onRemoveCategoryBudget(item.category.id) },
                    enabled = !state.isWritingAnyBudget,
                    modifier = Modifier.testTag("category_budget_remove_${item.category.id}"),
                ) {
                    Text(stringResource(R.string.category_budget_remove), color = ExpenseManagerColors.expense)
                }
            }
        }
    }
}

@Composable
private fun CategoryBudgetEditor(
    editor: CategoryBudgetEditorState,
    state: HomeUiState,
    actions: HomeActions,
) {
    Surface(
        color = ExpenseManagerColors.primaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("category_budget_editor"),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(if (editor.isEditing) R.string.category_budget_edit else R.string.category_budget_add),
                style = MaterialTheme.typography.titleSmall,
            )
            if (editor.isEditing) {
                state.activeExpenseCategories.firstOrNull { it.id == editor.categoryId }?.let {
                    Text(it.name, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                val configuredIds = state.categoryBudgetItems.mapTo(mutableSetOf()) { it.category.id }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.activeExpenseCategories.filter { it.id !in configuredIds }.forEach { category ->
                        AssistChip(
                            onClick = { actions.onSelectCategoryBudgetCategory(category.id) },
                            label = { Text(category.name) },
                            leadingIcon = { CategoryIcon(category.name, category.type, 34.dp) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .testTag("category_budget_choice_${category.id}"),
                            enabled = !state.isWritingAnyBudget,
                            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                containerColor = if (category.id == editor.categoryId) {
                                    ExpenseManagerColors.surface
                                } else ExpenseManagerColors.primaryContainer,
                            ),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = editor.input,
                onValueChange = actions.onCategoryBudgetInput,
                modifier = Modifier.fillMaxWidth().testTag("category_budget_input"),
                singleLine = true,
                label = { Text(stringResource(R.string.category_budget_limit)) },
                suffix = { Text(stringResource(R.string.vnd_suffix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = editor.inputInvalid,
                enabled = !state.isWritingAnyBudget,
                supportingText = {
                    if (editor.inputInvalid) Text(stringResource(R.string.amount_invalid))
                },
                shape = RoundedCornerShape(14.dp),
            )
            if (editor.writeFailed) ErrorBanner(stringResource(R.string.category_budget_save_error))
            Button(
                onClick = actions.onSaveCategoryBudget,
                enabled = !state.isWritingAnyBudget,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("category_budget_save"),
            ) {
                Text(stringResource(if (state.isSavingCategoryBudget) R.string.saving_transaction else R.string.home_save_budget))
            }
            TextButton(
                onClick = actions.onDismissCategoryBudgetEditor,
                enabled = !state.isWritingAnyBudget,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("category_budget_cancel"),
            ) { Text(stringResource(R.string.cancel)) }
        }
    }
}

@Composable
private fun AdaptiveCategoryBudgetHeading(text: String, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        val availableWidth = with(density) { maxWidth.toPx() }
        val fontSize = remember(text, availableWidth, density.fontScale) {
            var value = style.fontSize.value
            while (value > 10f && measurer.measure(
                    text,
                    style = style.copy(fontSize = value.sp),
                    maxLines = 1,
                    softWrap = false,
                ).size.width > availableWidth
            ) {
                value -= 0.5f
            }
            value.sp
        }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

private fun categoryBudgetPercent(budget: CategoryBudget, locale: Locale): String =
    NumberFormat.getPercentInstance(locale).format(BigDecimal(budget.usedPercent, 2))
