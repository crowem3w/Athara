package soc.athara.app.modules.business.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import soc.athara.app.modules.business.model.Booking
import soc.athara.app.modules.business.model.BookingStatus
import soc.athara.app.modules.business.model.BusinessState
import soc.athara.app.modules.business.model.dayLabel
import soc.athara.app.modules.business.model.peso
import soc.athara.app.modules.business.ui.AppCard
import soc.athara.app.modules.business.ui.EmptyState
import soc.athara.app.modules.business.ui.PageTitle
import soc.athara.app.modules.business.ui.RowDivider
import soc.athara.app.modules.business.ui.StatusChip
import soc.athara.app.modules.business.ui.Tone
import soc.athara.app.shared.designsystem.theme.Background
import soc.athara.app.shared.designsystem.theme.SurfaceMuted
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TealLight
import soc.athara.app.shared.designsystem.theme.TextPrimary
import soc.athara.app.shared.designsystem.theme.TextSecondary

private fun BookingStatus.tone() = when (this) {
    BookingStatus.Pending -> Tone.Warn
    BookingStatus.Confirmed -> Tone.Info
    BookingStatus.InProgress -> Tone.Info
    BookingStatus.Completed -> Tone.Good
    BookingStatus.Cancelled -> Tone.Bad
}

private val RejectReasons = listOf("Fully booked", "Outside operating hours", "Cannot meet the request", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookingsTab(
    state: BusinessState,
    initialFilter: BookingStatus?,
    openAvailability: Boolean,
    onAvailabilityHandled: () -> Unit,
    listState: LazyListState,
    padding: PaddingValues,
    onMessage: (String) -> Unit,
) {
    var filter by rememberSaveable(initialFilter) { mutableStateOf(initialFilter) }
    var query by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedRef by rememberSaveable { mutableStateOf<String?>(null) }
    var showAvailability by rememberSaveable { mutableStateOf(false) }
    var rejectingRef by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(openAvailability) {
        if (openAvailability) {
            showAvailability = true
            onAvailabilityHandled()
        }
    }

    val visible = state.bookings.filter { b ->
        (filter == null || b.status == filter) &&
            (day == null || b.day == day) &&
            (query.isBlank() || listOf(b.customer, b.offering, b.ref).any { it.contains(query, ignoreCase = true) })
    }

    LazyColumn(state = listState, contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Bookings") }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search guest, offering or reference") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                shape = RoundedCornerShape(14.dp),
            )
        }

        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf<BookingStatus?>(null) + BookingStatus.entries).forEach { s ->
                    val count = if (s == BookingStatus.Pending) state.pendingCount else 0
                    val label = (s?.label ?: "All") + if (count > 0) " ($count)" else ""
                    FilterChip(selected = filter == s, onClick = { filter = s }, label = { Text(label) })
                }
            }
        }

        item {
            WeekStrip(
                selected = day,
                hasBookings = { d -> state.bookings.any { it.day == d } },
                onSelect = { day = if (day == it) null else it },
            )
        }

        if (visible.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.EventBusy,
                    title = "No bookings here",
                    body = "Try another status or day. New requests appear as soon as customers book.",
                    actionLabel = "Clear filters",
                    onAction = { filter = null; day = null; query = "" },
                )
            }
        } else {
            items(visible, key = { it.ref }) { b -> BookingCard(b) { selectedRef = b.ref } }
        }
    }

    val current = selectedRef?.let { ref -> state.bookings.firstOrNull { it.ref == ref } }
    if (current != null) {
        BookingSheet(
            booking = current,
            onDismiss = { selectedRef = null },
            onAccept = { state.setStatus(current.ref, BookingStatus.Confirmed); onMessage("${current.ref} accepted") },
            onReject = { rejectingRef = current.ref },
            onStart = { state.setStatus(current.ref, BookingStatus.InProgress) },
            onComplete = { state.setStatus(current.ref, BookingStatus.Completed); onMessage("${current.ref} completed") },
            onCancel = { rejectingRef = current.ref },
        )
    }

    rejectingRef?.let { ref ->
        var reason by remember { mutableStateOf(RejectReasons.first()) }
        AlertDialog(
            onDismissRequest = { rejectingRef = null },
            title = { Text("Cancel this booking?") },
            text = {
                Column {
                    Text("The customer will be told why.", color = TextSecondary)
                    RejectReasons.forEach { r ->
                        Row(
                            Modifier.fillMaxWidth().clickable { reason = r },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = reason == r, onClick = { reason = r })
                            Text(r)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    state.setStatus(ref, BookingStatus.Cancelled)
                    rejectingRef = null
                    onMessage("$ref cancelled")
                }) { Text("Cancel booking", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { rejectingRef = null }) { Text("Keep booking") } },
        )
    }

    if (showAvailability) {
        AvailabilitySheet(
            onDismiss = { showAvailability = false },
            onSave = { showAvailability = false; onMessage("Availability saved") },
        )
    }
}

@Composable
private fun WeekStrip(selected: Int?, hasBookings: (Int) -> Boolean, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (d in 0..6) {
            val date = LocalDate.now().plusDays(d.toLong())
            val isSelected = selected == d
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Teal else SurfaceMuted)
                    .clickable { onSelect(d) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val fg = if (isSelected) Background else TextPrimary
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Background else TextSecondary,
                )
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge, color = fg)
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (hasBookings(d)) (if (isSelected) Background else Teal) else Background.copy(alpha = 0f)),
                )
            }
        }
    }
}

@Composable
private fun BookingCard(b: Booking, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(TealLight), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Event, contentDescription = null, tint = Teal)
            }
            Column(Modifier.weight(1f)) {
                Text(b.offering, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("${b.customer} \u2022 ${b.guests} guest${if (b.guests == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            StatusChip(b.status.label, b.status.tone())
        }
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Text(
                "${dayLabel(b.day)}, ${b.time}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 6.dp).weight(1f),
            )
            Text(peso(b.total), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookingSheet(
    booking: Booking,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Background) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(booking.ref, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
                StatusChip(booking.status.label, booking.status.tone())
            }
            Text(booking.offering, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            AppCard(padding = 0.dp) {
                DetailLine("Customer", booking.customer)
                RowDivider()
                DetailLine("When", "${dayLabel(booking.day)}, ${booking.time}")
                RowDivider()
                DetailLine("Guests", booking.guests.toString())
                RowDivider()
                DetailLine("Total", peso(booking.total))
                RowDivider()
                DetailLine("Payment", if (booking.paid) "Paid" else "Pay on arrival")
            }

            if (booking.note != null) {
                AppCard {
                    Text("Customer note", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(booking.note, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                }
            }

            when (booking.status) {
                BookingStatus.Pending -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Reject") }
                    Button(onClick = onAccept, modifier = Modifier.weight(1f)) { Text("Accept") }
                }
                BookingStatus.Confirmed -> {
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Start") }
                    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel booking", color = MaterialTheme.colorScheme.error)
                    }
                }
                BookingStatus.InProgress -> Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) { Text("Mark completed") }
                else -> {}
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

/** Weekly open days plus a per-slot capacity. Persist through the backend when it exists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailabilitySheet(onDismiss: () -> Unit, onSave: () -> Unit) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val open = remember { mutableStateOf(List(7) { true }) }
    var capacity by remember { mutableIntStateOf(10) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Background) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Availability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            AppCard(padding = 0.dp) {
                days.forEachIndexed { i, d ->
                    if (i > 0) RowDivider()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(d, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(if (open.value[i]) "Open" else "Closed", style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(end = 12.dp))
                        Switch(
                            checked = open.value[i],
                            onCheckedChange = { on -> open.value = open.value.toMutableList().also { it[i] = on } },
                        )
                    }
                }
            }
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Guests per time slot", style = MaterialTheme.typography.bodyLarge)
                        Text("Bookings stop when this is full.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    IconButton(onClick = { if (capacity > 1) capacity-- }) { Icon(Icons.Outlined.Remove, contentDescription = "Decrease") }
                    Text(capacity.toString(), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { capacity++ }) { Icon(Icons.Outlined.Add, contentDescription = "Increase") }
                }
            }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }
}
