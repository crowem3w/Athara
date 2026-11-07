package soc.athara.app.modules.business.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import soc.athara.app.modules.business.model.BusinessState
import soc.athara.app.modules.business.model.Offering
import soc.athara.app.modules.business.model.OfferingType
import soc.athara.app.modules.business.model.peso
import soc.athara.app.modules.business.ui.AppCard
import soc.athara.app.modules.business.ui.EmptyState
import soc.athara.app.modules.business.ui.IconRow
import soc.athara.app.modules.business.ui.PageTitle
import soc.athara.app.modules.business.ui.RowDivider
import soc.athara.app.modules.business.ui.SectionTitle
import soc.athara.app.modules.business.ui.StatusChip
import soc.athara.app.modules.business.ui.Tone
import soc.athara.app.shared.designsystem.theme.Background
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TealLight
import soc.athara.app.shared.designsystem.theme.TextPrimary
import soc.athara.app.shared.designsystem.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StoreTab(
    state: BusinessState,
    businessName: String,
    launchAdd: Boolean,
    onLaunchHandled: () -> Unit,
    listState: LazyListState,
    padding: PaddingValues,
    onMessage: (String) -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(OfferingType.Service) }
    var query by rememberSaveable { mutableStateOf("") }
    // 0 = closed, -1 = new offering, otherwise the id being edited.
    var editing by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(launchAdd) {
        if (launchAdd) {
            editing = -1
            onLaunchHandled()
        }
    }

    val shown = state.offerings.filter { it.type == type && (query.isBlank() || it.name.contains(query, ignoreCase = true)) }
    val soon = { name: String -> onMessage("$name is coming in the next pass") }

    LazyColumn(state = listState, contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("My Store") }

        item {
            AppCard {
                Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(10.dp)).background(TealLight))
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Teal), contentAlignment = Alignment.Center) {
                        Text(businessName.trim().firstOrNull()?.uppercase() ?: "?", color = Background, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(businessName.ifBlank { "Your business" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Tours and local goods", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    StatusChip(if (state.isOpen) "Open" else "Closed", if (state.isOpen) Tone.Good else Tone.Neutral)
                }
                Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Open for customers", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = state.isOpen, onCheckedChange = { state.isOpen = it })
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { soon("Edit profile") }, modifier = Modifier.weight(1f)) { Text("Edit profile") }
                    OutlinedButton(onClick = { soon("Storefront preview") }, modifier = Modifier.weight(1f)) { Text("Preview") }
                }
            }
        }

        item {
            AppCard(padding = 0.dp) {
                IconRow(Icons.Outlined.Store, "Business details", subtitle = "Address, contact, description", onClick = { soon("Business details") })
                RowDivider()
                IconRow(Icons.Outlined.PhotoLibrary, "Gallery", subtitle = "Cover and photos", onClick = { soon("Gallery") })
                RowDivider()
                IconRow(Icons.Outlined.Schedule, "Operating hours", onClick = { soon("Operating hours") })
                RowDivider()
                IconRow(Icons.Outlined.LocalOffer, "Promotions", onClick = { soon("Promotions") })
                RowDivider()
                IconRow(Icons.Outlined.Star, "Reviews", onClick = { soon("Reviews") })
            }
        }

        item {
            SectionTitle("Products and services")
        }

        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                OfferingType.entries.forEachIndexed { i, t ->
                    SegmentedButton(
                        selected = type == t,
                        onClick = { type = t },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = OfferingType.entries.size),
                    ) { Text("${t.label} (${state.offerings.count { it.type == t }})") }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search ${type.label.lowercase()}") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
            )
        }

        item {
            Button(onClick = { editing = -1 }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Add offering", Modifier.padding(start = 8.dp))
            }
        }

        if (shown.isEmpty()) {
            item {
                EmptyState(
                    icon = if (type == OfferingType.Product) Icons.Outlined.Inventory2 else Icons.Outlined.Event,
                    title = if (query.isBlank()) "Nothing here yet" else "No matches",
                    body = if (query.isBlank()) "Add your first ${type.name.lowercase()} so customers can find it." else "Try a different search.",
                    actionLabel = if (query.isBlank()) "Add ${type.name.lowercase()}" else null,
                    onAction = { editing = -1 },
                )
            }
        } else {
            items(shown, key = { it.id }) { o ->
                OfferingCard(o, onClick = { editing = o.id }, onToggle = { state.save(o.copy(available = it)) })
            }
        }
    }

    if (editing != 0) {
        val initial = state.offerings.firstOrNull { it.id == editing }
        OfferingSheet(
            initial = initial,
            defaultType = type,
            onDismiss = { editing = 0 },
            onSave = { state.save(it); editing = 0; onMessage("${it.name} saved") },
            onDelete = initial?.let { o -> { state.delete(o.id); editing = 0; onMessage("${o.name} deleted") } },
        )
    }
}

@Composable
private fun OfferingCard(o: Offering, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(TealLight), contentAlignment = Alignment.Center) {
                Icon(if (o.type == OfferingType.Product) Icons.Outlined.Inventory2 else Icons.Outlined.Event, contentDescription = null, tint = Teal)
            }
            Column(Modifier.weight(1f)) {
                Text(o.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    peso(o.price) + when (o.type) {
                        OfferingType.Product -> "  \u2022  ${o.stock ?: 0} in stock"
                        OfferingType.Service -> "  \u2022  ${o.durationMin ?: 0} min"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (o.lowStock) StatusChip(if ((o.stock ?: 0) == 0) "Out of stock" else "Low stock", Tone.Warn, Modifier.padding(top = 6.dp))
            }
            Switch(checked = o.available, onCheckedChange = onToggle)
        }
    }
}

/** One form for both kinds; only the fields that matter for the chosen type are shown. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferingSheet(
    initial: Offering?,
    defaultType: OfferingType,
    onDismiss: () -> Unit,
    onSave: (Offering) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var type by remember { mutableStateOf(initial?.type ?: defaultType) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var price by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "") }
    var duration by remember { mutableStateOf(initial?.durationMin?.toString() ?: "") }
    var capacity by remember { mutableStateOf(initial?.capacity?.toString() ?: "") }
    var available by remember { mutableStateOf(initial?.available ?: true) }
    var tried by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val nameOk = name.isNotBlank()
    val priceOk = price.toLongOrNull() != null
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Background) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (initial == null) "Add offering" else "Edit offering", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (initial == null) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    OfferingType.entries.forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = type == t,
                            onClick = { type = t },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = OfferingType.entries.size),
                        ) { Text(t.name) }
                    }
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Name *") }, singleLine = true,
                isError = tried && !nameOk, supportingText = { if (tried && !nameOk) Text("Enter a name") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = price, onValueChange = { price = it.filter(Char::isDigit) }, label = { Text("Price (\u20B1) *") }, singleLine = true,
                keyboardOptions = number, isError = tried && !priceOk, supportingText = { if (tried && !priceOk) Text("Enter a price") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (type == OfferingType.Product) {
                OutlinedTextField(
                    value = stock, onValueChange = { stock = it.filter(Char::isDigit) }, label = { Text("Stock quantity") }, singleLine = true,
                    keyboardOptions = number, modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = duration, onValueChange = { duration = it.filter(Char::isDigit) }, label = { Text("Duration (min)") }, singleLine = true,
                        keyboardOptions = number, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = capacity, onValueChange = { capacity = it.filter(Char::isDigit) }, label = { Text("Capacity") }, singleLine = true,
                        keyboardOptions = number, modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Available to customers", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = available, onCheckedChange = { available = it })
            }

            Button(
                onClick = {
                    tried = true
                    if (nameOk && priceOk) {
                        onSave(
                            Offering(
                                id = initial?.id ?: 0,
                                name = name.trim(),
                                type = type,
                                price = price.toLong(),
                                available = available,
                                stock = if (type == OfferingType.Product) stock.toIntOrNull() ?: 0 else null,
                                durationMin = if (type == OfferingType.Service) duration.toIntOrNull() else null,
                                capacity = if (type == OfferingType.Service) capacity.toIntOrNull() else null,
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }

            if (onDelete != null) {
                if (!confirmDelete) {
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete offering", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { confirmDelete = false }, modifier = Modifier.weight(1f)) { Text("Keep") }
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("Delete") }
                    }
                }
            }
        }
    }
}
