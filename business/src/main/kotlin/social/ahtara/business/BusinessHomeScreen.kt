package social.ahtara.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import social.ahtara.travel.model.PlaceCategory
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

private enum class BusinessDialog { CUSTOMER, ORDER, LISTING, EVENT }

private fun formatMoney(cents: Long): String = NumberFormat.getCurrencyInstance().format(BigDecimal.valueOf(cents, 2))

@Composable
internal fun BusinessHomeScreen(
    state: BusinessUiState,
    onAddCustomer: (name: String) -> Unit,
    onAddOrder: (title: String, customer: String, amountCents: Long) -> Unit,
    onDeleteCustomer: (Long) -> Unit,
    onDeleteOrder: (Long) -> Unit,
    onSaveListing: (
        importedId: String?, name: String, category: String, latitude: Double, longitude: Double,
        description: String, hours: String, phone: String, website: String, onResult: (Boolean) -> Unit,
    ) -> Unit,
    onDeleteListing: (Long) -> Unit,
    onAddEvent: (
        title: String, description: String, startMillis: Long, endMillis: Long?,
        locationName: String, latitude: Double?, longitude: Double?, website: String,
    ) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    modifier: Modifier = Modifier,
    
    extraContent: LazyListScope.() -> Unit = {},
) {
    var dialog by remember { mutableStateOf<BusinessDialog?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(R.string.business_stat_orders, state.orders.size.toString(), Modifier.weight(1f))
                    StatTile(R.string.business_stat_customers, state.customers.size.toString(), Modifier.weight(1f))
                    StatTile(R.string.business_stat_revenue, formatMoney(state.revenueCents), Modifier.weight(1f))
                }
            }

            
            item { SectionHeader(R.string.business_section_orders) { dialog = BusinessDialog.ORDER } }
            if (state.orders.isEmpty()) item { Text(stringResource(R.string.business_empty_orders), style = MaterialTheme.typography.bodyMedium) }
            items(state.orders, key = { "order-${it.id}" }) { order ->
                val date = DateFormat.getDateInstance().format(Date(order.createdAt))
                ItemRow(
                    primary = order.title,
                    secondary = listOfNotNull(order.customerName, date).joinToString(" • "),
                    trailing = formatMoney(order.amountCents),
                    onDelete = { onDeleteOrder(order.id) },
                )
            }

            
            item { SectionHeader(R.string.business_section_customers) { dialog = BusinessDialog.CUSTOMER } }
            if (state.customers.isEmpty()) item { Text(stringResource(R.string.business_empty_customers), style = MaterialTheme.typography.bodyMedium) }
            items(state.customers, key = { "customer-${it.id}" }) { customer ->
                ItemRow(
                    primary = customer.name,
                    secondary = null,
                    trailing = null,
                    onDelete = { onDeleteCustomer(customer.id) },
                )
            }

            
            item { SectionHeader(R.string.business_section_listings) { dialog = BusinessDialog.LISTING } }
            if (state.listings.isEmpty()) item { Text(stringResource(R.string.business_empty_listings), style = MaterialTheme.typography.bodyMedium) }
            items(state.listings, key = { "listing-${it.id}" }) { l ->
                ItemRow(
                    primary = l.name,
                    secondary = listOfNotNull(PlaceCategory.fromName(l.category)?.label, l.hours).joinToString(" • "),
                    trailing = null,
                    onDelete = { onDeleteListing(l.id) },
                )
            }

            
            item { SectionHeader(R.string.business_section_events) { dialog = BusinessDialog.EVENT } }
            if (state.events.isEmpty()) item { Text(stringResource(R.string.business_empty_events), style = MaterialTheme.typography.bodyMedium) }
            items(state.events, key = { "event-${it.id}" }) { e ->
                ItemRow(
                    primary = e.title,
                    secondary = DateFormat.getDateInstance().format(Date(e.startMillis)),
                    trailing = null,
                    onDelete = { onDeleteEvent(e.id) },
                )
            }

            extraContent()
        }
    }

    when (dialog) {
        BusinessDialog.CUSTOMER -> AddCustomerDialog(onDismiss = { dialog = null }, onSave = onAddCustomer)
        BusinessDialog.ORDER -> AddOrderDialog(onDismiss = { dialog = null }, onSave = onAddOrder)
        BusinessDialog.LISTING -> AddListingDialog(onDismiss = { dialog = null }, onSave = onSaveListing)
        BusinessDialog.EVENT -> AddEventDialog(onDismiss = { dialog = null }, onSave = onAddEvent)
        null -> Unit
    }
}

@Composable
private fun StatTile(label: Int, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SectionHeader(title: Int, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onAdd) { Text(stringResource(R.string.business_action_add)) }
    }
}

@Composable
private fun ItemRow(primary: String, secondary: String?, trailing: String?, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(primary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!secondary.isNullOrBlank()) Text(secondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) Text(trailing, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp))
            TextButton(onClick = onDelete) { Text(stringResource(R.string.business_action_delete)) }
        }
    }
}

@Composable
private fun AddCustomerDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.business_dialog_customer_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.business_label_customer_name)) },
                    singleLine = true, isError = nameError,
                    supportingText = if (nameError) ({ Text(stringResource(R.string.business_error_required)) }) else null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) nameError = true else { onSave(name); onDismiss() }
            }) { Text(stringResource(R.string.business_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.business_action_cancel)) } },
    )
}

@Composable
private fun AddOrderDialog(onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    var title by remember { mutableStateOf("") }
    var customer by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.business_dialog_order_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it; titleError = false },
                    label = { Text(stringResource(R.string.business_label_order_title)) },
                    singleLine = true, isError = titleError,
                    supportingText = if (titleError) ({ Text(stringResource(R.string.business_error_required)) }) else null,
                )
                OutlinedTextField(
                    value = customer, onValueChange = { customer = it },
                    label = { Text(stringResource(R.string.business_label_order_customer)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it; amountError = false },
                    label = { Text(stringResource(R.string.business_label_order_amount)) },
                    singleLine = true, isError = amountError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = if (amountError) ({ Text(stringResource(R.string.business_error_amount)) }) else null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = parseAmountCents(amount)
                titleError = title.isBlank()
                amountError = cents == null
                if (!titleError && cents != null) { onSave(title, customer, cents); onDismiss() }
            }) { Text(stringResource(R.string.business_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.business_action_cancel)) } },
    )
}


private val LISTING_CATEGORIES = PlaceCategory.entries.filter { it != PlaceCategory.EVENTS }

@Composable
private fun AddListingDialog(
    onDismiss: () -> Unit,
    onSave: (String?, String, String, Double, Double, String, String, String, String, (Boolean) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PlaceCategory.FOOD_DRINKS) }
    var lat by remember { mutableStateOf("") }
    var lon by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var coordError by remember { mutableStateOf(false) }
    var claimedError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.business_dialog_listing_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it; nameError = false },
                    label = { Text(stringResource(R.string.business_label_name)) },
                    singleLine = true, isError = nameError,
                    supportingText = if (nameError) ({ Text(stringResource(R.string.business_error_required)) }) else null,
                )
                Text(stringResource(R.string.business_label_category), style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LISTING_CATEGORIES.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                    }
                }
                OutlinedTextField(
                    value = lat, onValueChange = { lat = it; coordError = false },
                    label = { Text(stringResource(R.string.business_label_latitude)) },
                    singleLine = true, isError = coordError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = lon, onValueChange = { lon = it; coordError = false },
                    label = { Text(stringResource(R.string.business_label_longitude)) },
                    singleLine = true, isError = coordError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        Text(stringResource(if (coordError) R.string.business_error_coordinates else R.string.business_hint_coordinates))
                    },
                )
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.business_label_description)) })
                OutlinedTextField(value = hours, onValueChange = { hours = it }, singleLine = true,
                    label = { Text(stringResource(R.string.business_label_hours)) })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    label = { Text(stringResource(R.string.business_label_phone)) })
                OutlinedTextField(
                    value = website, onValueChange = { website = it }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    label = { Text(stringResource(R.string.business_label_website)) },
                    isError = claimedError,
                    supportingText = if (claimedError) ({ Text(stringResource(R.string.business_error_already_claimed)) }) else null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val la = lat.trim().replace(',', '.').toDoubleOrNull()
                val lo = lon.trim().replace(',', '.').toDoubleOrNull()
                nameError = name.isBlank()
                coordError = la == null || lo == null || la !in -90.0..90.0 || lo !in -180.0..180.0
                if (!nameError && !coordError) {
                    onSave(null, name, category.name, la!!, lo!!, description, hours, phone, website) { ok ->
                        if (ok) onDismiss() else claimedError = true
                    }
                }
            }) { Text(stringResource(R.string.business_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.business_action_cancel)) } },
    )
}

@Composable
private fun AddEventDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long?, String, Double?, Double?, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var where by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var startError by remember { mutableStateOf(false) }
    var endError by remember { mutableStateOf<Int?>(null) }
    val endErrorRes = endError
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.business_dialog_event_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it; titleError = false },
                    label = { Text(stringResource(R.string.business_label_event_title)) },
                    singleLine = true, isError = titleError,
                    supportingText = if (titleError) ({ Text(stringResource(R.string.business_error_required)) }) else null,
                )
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.business_label_description)) })
                OutlinedTextField(
                    value = start, onValueChange = { start = it; startError = false },
                    label = { Text(stringResource(R.string.business_label_start_date)) },
                    singleLine = true, isError = startError,
                    supportingText = if (startError) ({ Text(stringResource(R.string.business_error_date)) }) else null,
                )
                OutlinedTextField(
                    value = end, onValueChange = { end = it; endError = null },
                    label = { Text(stringResource(R.string.business_label_end_date)) },
                    singleLine = true, isError = endError != null,
                    supportingText = if (endErrorRes != null) ({ Text(stringResource(endErrorRes)) }) else null,
                )
                OutlinedTextField(value = where, onValueChange = { where = it }, singleLine = true,
                    label = { Text(stringResource(R.string.business_label_event_location)) })
                OutlinedTextField(
                    value = website, onValueChange = { website = it }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    label = { Text(stringResource(R.string.business_label_website)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startMillis = parseDateMillis(start)
                
                val endMillis = if (end.isBlank()) null else parseDateMillis(end)?.plus(24L * 60 * 60 * 1000 - 1)
                titleError = title.isBlank()
                startError = startMillis == null
                endError = when {
                    end.isNotBlank() && endMillis == null -> R.string.business_error_date
                    startMillis != null && endMillis != null && endMillis < startMillis -> R.string.business_error_end_before_start
                    else -> null
                }
                if (!titleError && startMillis != null && endError == null) {
                    onSave(title, description, startMillis, endMillis, where, null, null, website)
                    onDismiss()
                }
            }) { Text(stringResource(R.string.business_action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.business_action_cancel)) } },
    )
}