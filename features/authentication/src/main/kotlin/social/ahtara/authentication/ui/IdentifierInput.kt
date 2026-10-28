package social.ahtara.authentication.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.Countries
import social.ahtara.authentication.domain.Country





@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IdentifierInput(
    state: AuthUiState,
    onMobile: (String) -> Unit,
    onCountry: (Country) -> Unit,
    imeAction: ImeAction,
    pill: Boolean = false,
) {
    var picking by remember { mutableStateOf(false) }
    val container = colorResource(R.color.authentication_field_background)
    val colors = if (pill) OutlinedTextFieldDefaults.colors(
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        errorContainerColor = container,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = colorResource(R.color.authentication_field_border),
    ) else OutlinedTextFieldDefaults.colors()
    val shape = if (pill) CircleShape else OutlinedTextFieldDefaults.shape
    val error = state.identifierError

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactTextField(
            value = state.mobile,
            onValueChange = onMobile,
            placeholder = stringResource(
                if (state.country.iso == "PH") R.string.authentication_hint_mobile_ph
                else R.string.authentication_hint_mobile
            ),
            leadingIcon = {
                val change = stringResource(R.string.authentication_country_change)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClickLabel = change) { picking = true }
                        .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Text(state.country.flag, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(6.dp))
                    Text(state.country.dialLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            error = error,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = imeAction),
            keyboardActions = KeyboardActions(),
            shape = shape,
            colors = colors,
        )
    }

    if (picking) {
        CountryPickerDialog(
            selected = state.country,
            onSelect = { onCountry(it); picking = false },
            onDismiss = { picking = false },
        )
    }
}






@Composable
internal fun LoginIdInput(
    value: String,
    onChange: (String) -> Unit,
    error: Int?,
    imeAction: ImeAction,
    pill: Boolean = false,
) {
    val container = colorResource(R.color.authentication_field_background)
    val muted = colorResource(R.color.authentication_muted)
    CompactTextField(
        value = value,
        onValueChange = onChange,
        placeholder = stringResource(R.string.authentication_hint_login_id),
        leadingIcon = if (pill) {
            { Icon(Icons.Outlined.Person, contentDescription = null) }
        } else null,
        error = error,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = imeAction),
        keyboardActions = KeyboardActions(),
        shape = if (pill) CircleShape else OutlinedTextFieldDefaults.shape,
        colors = if (pill) OutlinedTextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            errorContainerColor = container,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = colorResource(R.color.authentication_field_border),
            focusedLeadingIconColor = muted,
            unfocusedLeadingIconColor = muted,
            focusedPlaceholderColor = muted,
            unfocusedPlaceholderColor = muted,
        ) else OutlinedTextFieldDefaults.colors(),
    )
}





@Composable
internal fun PhMobileInput(
    value: String,
    onChange: (String) -> Unit,
    error: Int?,
    imeAction: ImeAction,
) {
    CompactTextField(
        value = value,
        onValueChange = onChange,
        placeholder = stringResource(R.string.authentication_hint_mobile_optional),
        leadingIcon = {
            Text(
                Countries.DEFAULT.dialLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, end = 8.dp),
            )
        },
        error = error,
        visualTransformation = PhMobileTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = imeAction),
        keyboardActions = KeyboardActions(),
    )
}


private object PhMobileTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val spaced = buildString {
            digits.forEachIndexed { i, c ->
                if (i == 3 || i == 6) append(' ')
                append(c)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 6 -> offset + 1
                else -> offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 3 -> offset
                offset <= 7 -> offset - 1
                else -> offset - 2
            }.coerceIn(0, digits.length)
        }
        return TransformedText(AnnotatedString(spaced), mapping)
    }
}

@Composable
private fun CountryPickerDialog(selected: Country, onSelect: (Country) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().removePrefix("+").lowercase()
    val results = remember(q) {
        if (q.isEmpty()) emptyList()
        else Countries.all.filter { it.name.lowercase().contains(q) || it.dialCode.startsWith(q) || it.iso.lowercase() == q }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 560.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.authentication_country_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.authentication_country_close))
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.authentication_country_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    if (q.isEmpty()) {
                        item { SectionLabel(R.string.authentication_country_popular) }
                        items(Countries.popular, key = { "p" + it.iso }) { CountryRow(it, it == selected, onSelect) }
                        item { SectionLabel(R.string.authentication_country_all) }
                        items(Countries.all, key = { it.iso }) { CountryRow(it, it == selected, onSelect) }
                    } else if (results.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.authentication_country_none),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(20.dp),
                            )
                        }
                    } else {
                        items(results, key = { it.iso }) { CountryRow(it, it == selected, onSelect) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = colorResource(R.color.authentication_muted),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun CountryRow(country: Country, isSelected: Boolean, onSelect: (Country) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(country) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(country.flag, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Text(country.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(country.dialLabel, style = MaterialTheme.typography.bodyMedium, color = colorResource(R.color.authentication_muted))
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
    HorizontalDivider(color = colorResource(R.color.authentication_field_border).copy(alpha = 0.4f))
}