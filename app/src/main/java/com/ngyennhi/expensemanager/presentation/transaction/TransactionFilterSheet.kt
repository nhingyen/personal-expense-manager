package com.ngyennhi.expensemanager.presentation.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.TransactionDateRange
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionFilterSheet(
    state: TransactionUiState,
    onDismiss: () -> Unit,
    onApply: (String?, TransactionDateRange?) -> Unit,
    onReset: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    var category by rememberSaveable { mutableStateOf(state.selectedCategory) }
    var startDay by rememberSaveable { mutableStateOf(state.dateRange?.start?.toEpochDay()) }
    var endDay by rememberSaveable { mutableStateOf(state.dateRange?.endInclusive?.toEpochDay()) }
    var showCategories by rememberSaveable { mutableStateOf(false) }
    var editingStart by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val start = startDay?.let(LocalDate::ofEpochDay)
    val end = endDay?.let(LocalDate::ofEpochDay)
    val customPeriod = start != null && end != null
    val validPeriod = start == null || end == null || !end.isBefore(start)
    val range = if (start != null && end != null && validPeriod) TransactionDateRange(start, end) else null
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = ExpenseManagerColors.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag("transaction_filter_sheet")) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.transaction_filters), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.transaction_category), style = MaterialTheme.typography.labelMedium)
            Box {
                OutlinedButton(onClick = { showCategories = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("filter_category")) {
                    Text(category ?: stringResource(R.string.filter_all_categories))
                }
                DropdownMenu(expanded = showCategories, onDismissRequest = { showCategories = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.filter_all_categories)) },
                        modifier = Modifier.testTag("filter_category_all"),
                        onClick = { category = null; showCategories = false })
                    state.availableCategories.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            category = option
                            showCategories = false
                        })
                    }
                }
            }
            Text(stringResource(R.string.filter_period), style = MaterialTheme.typography.labelMedium)
            TypeChip(stringResource(R.string.filter_selected_month, formatMonth(state.selectedMonth, locale)),
                !customPeriod, { startDay = null; endDay = null },
                Modifier.fillMaxWidth().testTag("filter_month"))
            TypeChip(stringResource(R.string.filter_custom_range), customPeriod, {
                if (!customPeriod) {
                    startDay = state.period.start.toEpochDay()
                    endDay = state.period.endInclusive.toEpochDay()
                }
            }, Modifier.fillMaxWidth().testTag("filter_date_range"))
            if (start != null && end != null) {
                OutlinedButton(onClick = { editingStart = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("filter_start_date")) {
                    Text(stringResource(R.string.filter_start_date_value, formatDate(start, locale)))
                }
                OutlinedButton(onClick = { editingStart = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("filter_end_date")) {
                    Text(stringResource(R.string.filter_end_date_value, formatDate(end, locale)))
                }
                if (!validPeriod) Text(stringResource(R.string.filter_invalid_range),
                    color = ExpenseManagerColors.expense, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
            }
            Button(onClick = { onApply(category, range) }, enabled = validPeriod,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("apply_transaction_filters")) {
                Text(stringResource(R.string.apply_filters))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onReset, modifier = Modifier.heightIn(min = 48.dp).testTag("sheet_reset_filters")) {
                    Text(stringResource(R.string.reset_filters))
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("cancel_transaction_filters")) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
    editingStart?.let { isStart ->
        TransactionDatePicker(
            initialDate = if (isStart) requireNotNull(start) else requireNotNull(end),
            onDismiss = { editingStart = null }, onConfirm = {
                if (isStart) startDay = it.toEpochDay() else endDay = it.toEpochDay()
                editingStart = null
            })
    }
}
