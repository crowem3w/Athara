package social.ahtara.authentication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.Validators


internal class BirthdayActions(
    val onMonth: (String) -> Unit,
    val onDay: (String) -> Unit,
    val onYear: (String) -> Unit,
    
    val onDate: (Int, Int, Int) -> Unit,
) {
    companion object {
        val None = BirthdayActions(onMonth = {}, onDay = {}, onYear = {}, onDate = { _, _, _ -> })
    }
}






@Composable
internal fun BirthdayInput(
    month: String,
    day: String,
    year: String,
    error: Int?,
    actions: BirthdayActions,
) {
    var picking by remember { mutableStateOf(false) }
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DatePartField(
                value = month, placeholder = R.string.authentication_label_birth_month, maxLength = 2, padAbove = '1',
                hasError = error != null, onChange = actions.onMonth, modifier = Modifier.weight(1f),
            )
            DatePartField(
                value = day, placeholder = R.string.authentication_label_birth_day, maxLength = 2, padAbove = '3',
                hasError = error != null, onChange = actions.onDay, modifier = Modifier.weight(1f),
            )
            DatePartField(
                value = year, placeholder = R.string.authentication_label_birth_year, maxLength = 4, padAbove = null,
                hasError = error != null, onChange = actions.onYear, modifier = Modifier.weight(1.4f),
            )
            IconButton(onClick = { picking = true }, modifier = Modifier.size(FieldHeight)) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.authentication_birthday_pick),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        error?.let {
            Text(
                text = stringResource(it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
            )
        }
    }

    if (picking) {
        BirthdayPickerDialog(
            initial = parseDate(month, day, year),
            onConfirm = { date ->
                actions.onDate(date.monthValue, date.dayOfMonth, date.year)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun DatePartField(
    value: String,
    placeholder: Int,
    maxLength: Int,
    padAbove: Char?,
    hasError: Boolean,
    onChange: (String) -> Unit,
    modifier: Modifier,
) {
    val focus = LocalFocusManager.current
    CompactTextField(
        value = value,
        onValueChange = { raw ->
            var digits = raw.filter { it.isDigit() }.take(maxLength)
            
            if (padAbove != null && digits.length == 1 && value.isEmpty() && digits[0] > padAbove) digits = "0$digits"
            onChange(digits)
            if (digits.length == maxLength && value.length < maxLength) focus.moveFocus(FocusDirection.Next)
        },
        placeholder = stringResource(placeholder),
        forceError = hasError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayPickerDialog(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    
    val latest = remember { LocalDate.now().minusYears(Validators.MIN_AGE.toLong()) }
    val latestMillis = remember { latest.toUtcMillis() }
    val start = initial?.takeIf { it.year >= MinYear && !it.isAfter(latest) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = start?.toUtcMillis(),
        initialDisplayedMonthMillis = (start ?: latest).toUtcMillis(),
        yearRange = MinYear..latest.year,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= latestMillis
            override fun isSelectableYear(year: Int): Boolean = year <= latest.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onConfirm(it.toUtcDate()) } },
            ) { Text(stringResource(R.string.authentication_action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.authentication_action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

private const val MinYear = 1900


private fun parseDate(month: String, day: String, year: String): LocalDate? = runCatching {
    LocalDate.of(year.toInt(), month.toInt(), day.toInt())
}.getOrNull()


private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()