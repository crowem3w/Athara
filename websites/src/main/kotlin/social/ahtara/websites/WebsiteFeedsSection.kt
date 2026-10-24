package social.ahtara.websites

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import social.ahtara.travel.model.PlaceCategory





@Composable
fun WebsiteFeedsSection(ownerId: String, modifier: Modifier = Modifier) {
    val viewModel: WebsiteFeedsViewModel = viewModel(
        key = "website-feeds-$ownerId",
        factory = WebsiteFeedsViewModel.factory(ownerId),
    )
    val sites by viewModel.websites.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.websites_section_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { showDialog = true }) { Text(stringResource(R.string.websites_action_add)) }
        }
        if (sites.isEmpty()) Text(stringResource(R.string.websites_empty), style = MaterialTheme.typography.bodyMedium)
        sites.forEach { site ->
            val categoryLabel = PlaceCategory.fromName(site.category)?.label ?: stringResource(R.string.websites_category_auto)
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(site.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$categoryLabel • ${site.url}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { viewModel.delete(site.id) }) { Text(stringResource(R.string.websites_action_delete)) }
                }
            }
        }
    }

    if (showDialog) {
        AddWebsiteDialog(onDismiss = { showDialog = false }, onSave = viewModel::add)
    }
}

@Composable
private fun AddWebsiteDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, category: String?, lat: Double, lon: Double, onResult: (Boolean) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<PlaceCategory?>(null) }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }
    var coordError by remember { mutableStateOf(false) }
    var limitError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.websites_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false; limitError = false },
                    label = { Text(stringResource(R.string.websites_label_name)) },
                    singleLine = true, isError = nameError,
                    supportingText = if (nameError) ({ Text(stringResource(R.string.websites_error_required)) }) else null,
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it; urlError = false },
                    label = { Text(stringResource(R.string.websites_label_url)) },
                    singleLine = true, isError = urlError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = {
                        Text(stringResource(if (urlError) R.string.websites_error_url else R.string.websites_hint_url))
                    },
                )
                Text(stringResource(R.string.websites_label_category), style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = category == null, onClick = { category = null },
                        label = { Text(stringResource(R.string.websites_category_auto)) },
                    )
                    PlaceCategory.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
                Text(stringResource(R.string.websites_hint_category), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = lat, onValueChange = { lat = it; coordError = false },
                    label = { Text(stringResource(R.string.websites_label_latitude)) },
                    singleLine = true, isError = coordError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = lon, onValueChange = { lon = it; coordError = false },
                    label = { Text(stringResource(R.string.websites_label_longitude)) },
                    singleLine = true, isError = coordError || limitError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text(
                            when {
                                limitError -> stringResource(R.string.websites_error_limit, MAX_WEBSITES_PER_OWNER)
                                coordError -> stringResource(R.string.websites_error_coordinates)
                                else -> stringResource(R.string.websites_hint_coordinates)
                            }
                        )
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val normalized = normalizeWebsiteUrl(url)
                val la = lat.trim().replace(',', '.').toDoubleOrNull()
                val lo = lon.trim().replace(',', '.').toDoubleOrNull()
                nameError = name.isBlank()
                urlError = normalized == null
                coordError = la == null || lo == null || la !in -90.0..90.0 || lo !in -180.0..180.0
                if (!nameError && !urlError && !coordError) {
                    onSave(name, normalized!!, category?.name, la!!, lo!!) { ok ->
                        if (ok) onDismiss() else limitError = true
                    }
                }
            }) { Text(stringResource(R.string.websites_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.websites_action_cancel)) } },
    )
}