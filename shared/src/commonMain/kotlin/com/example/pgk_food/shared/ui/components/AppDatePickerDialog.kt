package com.example.pgk_food.shared.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    visible: Boolean,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    isDateSelectable: (LocalDate) -> Boolean = { true },
) {
    if (!visible) return

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toDatePickerUtcMillis(),
        selectableDates = AppSelectableDates(isDateSelectable)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis
                    ?.toLocalDateFromPickerMillis()
                    ?.takeIf(isDateSelectable)
                    ?.let(onDateSelected)
                onDismiss()
            }) { Text("ОК") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private class AppSelectableDates(
    private val isSelectable: (LocalDate) -> Boolean,
) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return isSelectable(utcTimeMillis.toLocalDateFromPickerMillis())
    }

    override fun isSelectableYear(year: Int): Boolean = year in 2020..2100
}

fun LocalDate.toDatePickerUtcMillis(): Long {
    return atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}

fun Long.toLocalDateFromPickerMillis(): LocalDate {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date
}
