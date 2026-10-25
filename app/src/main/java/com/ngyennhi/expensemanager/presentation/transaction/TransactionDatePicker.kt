package com.ngyennhi.expensemanager.presentation.transaction

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.ngyennhi.expensemanager.R
import java.time.LocalDate

/** Shared by the transaction form and the filter's independent start/end dates. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDatePicker(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDay() * 86_400_000L,
        yearRange = minOf(1900, initialDate.year)..maxOf(2100, initialDate.year),
    )
    DatePickerDialog(
        modifier = Modifier.testTag("transaction_date_picker"),
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = picker.selectedDateMillis != null,
                modifier = Modifier.testTag("confirm_transaction_date"), onClick = {
                    // Material uses UTC midnight, independent of the device timezone.
                    picker.selectedDateMillis?.let { onConfirm(LocalDate.ofEpochDay(Math.floorDiv(it, 86_400_000L))) }
                }) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_transaction_date")) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) { DatePicker(state = picker) }
}
