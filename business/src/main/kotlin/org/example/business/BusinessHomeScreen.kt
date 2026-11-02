package org.example.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import org.example.authentication.domain.AccountType
import org.example.authentication.domain.AuthUser
import org.example.authentication.ui.AccountTypeBadge

private enum class BusinessDialog { CUSTOMER, ORDER }

private fun formatMoney(cents: Long): String = NumberFormat.getCurrencyInstance().format(BigDecimal.valueOf(cents, 2))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BusinessHomeScreen(
    user: AuthUser,
    state: BusinessUiState,
    onAddCustomer: (name: String) -> Unit,
    onAddOrder: (title: String, customer: String, amountCents: Long) -> Unit,
    onDeleteCustomer: (Long) -> Unit,
    onDeleteOrder: (Long) -> Unit,
    onSignOut: () -> Unit,
) {
    var dialog by remember { mutableStateOf<BusinessDialog?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.business_home_title)) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AccountTypeBadge(AccountType.BUSINESS, size = 56.dp)
                    Text(
                        text = stringResource(R.string.business_home_greeting, user.displayName),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(R.string.business_stat_orders, state.orders.size.toString(), Modifier.weight(1f))
                    StatTile(R.string.business_stat_customers, state.customers.size.toString(), Modifier.weight(1f))
                    StatTile(R.string.business_stat_revenue, formatMoney(state.revenueCents), Modifier.weight(1f))
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.business_profile_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.business_profile_login, user.identifier))
                        user.details.industry?.let { Text(stringResource(R.string.business_profile_industry, it)) }
                        user.details.phone?.let { Text(stringResource(R.string.business_profile_phone, it)) }
                        user.details.registrationId?.let { Text(stringResource(R.string.business_profile_registration, it)) }
                        Text(stringResource(R.string.business_profile_type))
                    }
                }
            }

            // ----- Orders -----
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

            // ----- Customers -----
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

            item {
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.business_action_sign_out))
                }
            }
        }
    }

    when (dialog) {
        BusinessDialog.CUSTOMER -> AddCustomerDialog(onDismiss = { dialog = null }, onSave = onAddCustomer)
        BusinessDialog.ORDER -> AddOrderDialog(onDismiss = { dialog = null }, onSave = onAddOrder)
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
