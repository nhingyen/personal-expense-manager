package com.ngyennhi.expensemanager.presentation.transaction

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ngyennhi.expensemanager.R
import com.ngyennhi.expensemanager.domain.model.TransactionType
import com.ngyennhi.expensemanager.presentation.common.components.AppTopBar
import com.ngyennhi.expensemanager.ui.theme.ExpenseManagerColors
import java.text.NumberFormat
import java.time.LocalDate

@Composable
internal fun TransactionFormScreen(
    state: TransactionUiState,
    onBack: () -> Unit,
    onReturnToList: () -> Unit,
    onRetry: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val form = state.form
    val locale = LocalConfiguration.current.locales[0]
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    val vndSuffix = stringResource(R.string.vnd_suffix)
    Scaffold(
        containerColor = ExpenseManagerColors.background,
        topBar = { AppTopBar(stringResource(if (form.editingId == null) R.string.add_transaction_title
            else R.string.edit_transaction_title), onBack = onBack) },
    ) { padding ->
        when {
            form.editingId != null && state.loadStatus == LoadStatus.LOADING && !form.initialized ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            form.editingId != null && state.loadStatus == LoadStatus.ERROR && !form.initialized ->
                Box(Modifier.fillMaxSize().padding(padding).padding(20.dp), contentAlignment = Alignment.Center) {
                    ErrorBanner(stringResource(R.string.load_transactions_error), onRetry)
                }
            state.editRecordMissing -> UnavailableState(Modifier.padding(padding), onReturnToList)
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip(stringResource(R.string.type_expense), form.type == TransactionType.EXPENSE,
                        { onTypeChange(TransactionType.EXPENSE) }, Modifier.weight(1f))
                    TypeChip(stringResource(R.string.type_income), form.type == TransactionType.INCOME,
                        { onTypeChange(TransactionType.INCOME) }, Modifier.weight(1f))
                }
                Column {
                    Surface(color = ExpenseManagerColors.primaryContainer, shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.transaction_amount), style = MaterialTheme.typography.labelMedium,
                                color = ExpenseManagerColors.primaryText)
                            Spacer(Modifier.height(8.dp))
                            BasicTextField(
                                value = form.amountInput,
                                onValueChange = onAmountChange,
                                modifier = Modifier.fillMaxWidth().testTag("amount_input"),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.displaySmall.copy(
                                    color = ExpenseManagerColors.primaryText, textAlign = TextAlign.Center),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                cursorBrush = SolidColor(ExpenseManagerColors.primary),
                                visualTransformation = remember(vndSuffix) { VndSuffixTransformation(vndSuffix) },
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        if (form.amountInput.isEmpty()) Text(
                                            stringResource(R.string.amount_placeholder, NumberFormat.getIntegerInstance(locale).format(0)),
                                            style = MaterialTheme.typography.displaySmall,
                                            color = ExpenseManagerColors.textSecondary)
                                        inner()
                                    }
                                },
                            )
                        }
                    }
                    FieldErrorText(form.amountError?.let { if (it == FieldError.REQUIRED) R.string.field_required
                        else R.string.amount_invalid })
                }
                Column {
                    Text(stringResource(R.string.transaction_category), style = MaterialTheme.typography.labelMedium,
                        color = ExpenseManagerColors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedTextField(
                            value = form.category,
                            onValueChange = onCategoryChange,
                            modifier = Modifier.fillMaxWidth().testTag("category_input"),
                            placeholder = { Text(stringResource(R.string.category_hint)) },
                            leadingIcon = { CategoryIcon(form.category.ifBlank {
                                if (form.type == TransactionType.EXPENSE) "Food" else "Salary"
                            }, form.type, 36.dp) },
                            trailingIcon = {
                                IconButton(onClick = { showCategories = true },
                                    modifier = Modifier.testTag("choose_category_button")) {
                                    Image(painterResource(R.drawable.ic_chevron_down),
                                        stringResource(R.string.choose_category), Modifier.size(20.dp))
                                }
                            },
                            singleLine = true,
                            isError = form.categoryError != null,
                            shape = RoundedCornerShape(14.dp),
                        )
                        val categoryOptions = suggestedCategories(form.type)
                        DropdownMenu(expanded = showCategories, onDismissRequest = { showCategories = false }) {
                            categoryOptions.forEach { label ->
                                val categoryName = stringResource(label)
                                DropdownMenuItem(text = { Text(categoryName) }, onClick = {
                                    onCategoryChange(categoryName)
                                    showCategories = false
                                })
                            }
                        }
                    }
                    FieldErrorText(form.categoryError?.let { R.string.field_required })
                }
                Column {
                    Text(stringResource(R.string.transaction_date), style = MaterialTheme.typography.labelMedium,
                        color = ExpenseManagerColors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { showDatePicker = true },
                        color = ExpenseManagerColors.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseManagerColors.outline),
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Image(painterResource(R.drawable.ic_calendar), contentDescription = null, modifier = Modifier.size(22.dp))
                            Text(formatDate(form.date, locale), modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium)
                            Image(painterResource(R.drawable.ic_chevron_down), contentDescription = stringResource(R.string.pick_date),
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Column {
                    Text(stringResource(R.string.transaction_note), style = MaterialTheme.typography.labelMedium,
                        color = ExpenseManagerColors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = form.note,
                        onValueChange = onNoteChange,
                        modifier = Modifier.fillMaxWidth().testTag("note_input"),
                        placeholder = { Text(stringResource(R.string.note_hint)) },
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        supportingText = { Text(stringResource(R.string.note_count, form.note.length)) },
                    )
                }
                when (state.actionError) {
                    ActionError.SAVE_FAILED -> ErrorBanner(stringResource(R.string.save_failed), onSave)
                    ActionError.RECORD_UNAVAILABLE -> ErrorBanner(stringResource(R.string.record_unavailable), onReturnToList)
                    else -> Unit
                }
                Button(
                    onClick = onSave,
                    enabled = !form.isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_transaction_button"),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseManagerColors.primary),
                ) {
                    Image(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (form.isSaving) R.string.saving_transaction else R.string.save_transaction))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    if (showDatePicker) {
        TransactionDatePicker(form.date, onDismiss = { showDatePicker = false }, onConfirm = {
            onDateChange(it)
            showDatePicker = false
        })
    }
}

@Composable
private fun FieldErrorText(error: Int?) {
    if (error != null) {
        Text(stringResource(error), style = MaterialTheme.typography.bodySmall,
            color = ExpenseManagerColors.expense,
            modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Assertive })
    }
}

private class VndSuffixTransformation(private val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val originalLength = text.length
        return TransformedText(
            AnnotatedString(text.text + suffix),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset.coerceIn(0, originalLength)
                override fun transformedToOriginal(offset: Int): Int = offset.coerceIn(0, originalLength)
            },
        )
    }
}
