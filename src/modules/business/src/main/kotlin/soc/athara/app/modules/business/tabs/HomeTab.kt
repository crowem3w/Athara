package soc.athara.app.modules.business.tabs

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import soc.athara.app.modules.business.R
import soc.athara.app.modules.business.model.ActivityKind
import soc.athara.app.modules.business.model.BookingStatus
import soc.athara.app.modules.business.model.BusinessState
import soc.athara.app.modules.business.model.DashPeriod
import soc.athara.app.modules.business.model.SampleData
import soc.athara.app.modules.business.model.peso
import soc.athara.app.modules.business.ui.AppCard
import soc.athara.app.modules.business.ui.IconRow
import soc.athara.app.modules.business.ui.RowDivider
import soc.athara.app.modules.business.ui.SectionTitle
import soc.athara.app.modules.business.ui.StatusChip
import soc.athara.app.modules.business.ui.Tone
import soc.athara.app.modules.business.ui.TrendChart
import soc.athara.app.shared.designsystem.theme.SurfaceMuted
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TealDark
import soc.athara.app.shared.designsystem.theme.TealLight
import soc.athara.app.shared.designsystem.theme.TextPrimary
import soc.athara.app.shared.designsystem.theme.TextSecondary

internal class HomeActions(
    val onBookings: (BookingStatus?) -> Unit,
    val onAddOffering: () -> Unit,
    val onAvailability: () -> Unit,
    val onStore: () -> Unit,
    val onEarnings: () -> Unit,
)

private class Attention(val icon: ImageVector, val text: String, val action: String, val onClick: () -> Unit)

/**
 * Answers three questions in order: how is the business doing, what needs me, what next.
 * Attention items sit above the chart on purpose.
 */
@Composable
internal fun HomeTab(
    state: BusinessState,
    name: String,
    listState: LazyListState,
    padding: PaddingValues,
    actions: HomeActions,
) {
    var period by rememberSaveable { mutableStateOf(DashPeriod.Today) }
    val stats = state.stats(period)

    val attention = buildList {
        val pending = state.pendingCount
        if (pending > 0) {
            add(Attention(Icons.Outlined.EventAvailable, "$pending pending booking${if (pending == 1) "" else "s"}", "Review") {
                actions.onBookings(BookingStatus.Pending)
            })
        }
        if (state.newOrders > 0) {
            add(Attention(Icons.Outlined.ShoppingBag, "${state.newOrders} new orders to prepare", "View") { actions.onStore() })
        }
        val low = state.lowStock
        if (low.isNotEmpty()) {
            add(Attention(Icons.Outlined.Inventory2, "${low.size} item${if (low.size == 1) "" else "s"} running low", "Manage") { actions.onStore() })
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    greeting(name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Here is how your business is doing.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }

        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashPeriod.entries.forEach { p ->
                    FilterChip(selected = p == period, onClick = { period = p }, label = { Text(p.label) })
                }
            }
        }

        item {
            AppCard(onClick = actions.onEarnings) {
                Text("Revenue, ${period.label.lowercase()}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(peso(stats.revenue), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Bookings", stats.bookings.toString(), Modifier.weight(1f)) { actions.onBookings(null) }
                if (state.supportsOrders) StatTile("Orders", stats.orders.toString(), Modifier.weight(1f)) { actions.onStore() }
            }
        }

        item { SectionTitle("Needs your attention") }
        item {
            AppCard(padding = 0.dp) {
                if (attention.isEmpty()) {
                    IconRow(Icons.Outlined.CheckCircleOutline, "You are all caught up", subtitle = "New requests will show up here.")
                } else {
                    attention.forEachIndexed { i, a ->
                        if (i > 0) RowDivider()
                        IconRow(a.icon, a.text, onClick = a.onClick, trailing = { StatusChip(a.action, Tone.Info) })
                    }
                }
            }
        }

        item { SectionTitle("Quick actions") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile(Icons.Outlined.AddCircleOutline, "Add offering", Modifier.weight(1f), actions.onAddOffering)
                    ActionTile(Icons.Outlined.Schedule, "Availability", Modifier.weight(1f), actions.onAvailability)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionTile(Icons.Outlined.CalendarMonth, "Bookings", Modifier.weight(1f)) { actions.onBookings(null) }
                    ActionTile(Icons.Outlined.Storefront, "My storefront", Modifier.weight(1f), actions.onStore)
                }
            }
        }

        // Existing "collect payment" guidance, kept from the previous Home screen.
        item {
            Surface(shape = RoundedCornerShape(14.dp), color = TealLight, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Payments, contentDescription = null, tint = Teal)
                    Column {
                        Text(stringResource(R.string.business_collect_payment), style = MaterialTheme.typography.titleMedium, color = TealDark)
                        Text(
                            stringResource(R.string.business_collect_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        item { SectionTitle("Recent activity", actionLabel = "View all", onAction = actions.onEarnings) }
        item {
            AppCard(padding = 0.dp) {
                SampleData.activity.forEachIndexed { i, a ->
                    if (i > 0) RowDivider()
                    val icon = when (a.kind) {
                        ActivityKind.Payment -> Icons.Outlined.Payments
                        ActivityKind.Booking -> Icons.Outlined.EventAvailable
                        ActivityKind.Order -> Icons.Outlined.ShoppingBag
                    }
                    IconRow(icon, a.title, subtitle = a.detail, onClick = {
                        if (a.kind == ActivityKind.Booking) actions.onBookings(BookingStatus.Pending) else actions.onEarnings()
                    })
                }
            }
        }

        item { SectionTitle("Performance") }
        item {
            AppCard {
                Text("Revenue trend, ${period.label.lowercase()}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                TrendChart(stats.trend, Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    AppCard(modifier = modifier, onClick = onClick) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.heightIn(min = 64.dp), shape = RoundedCornerShape(14.dp), color = SurfaceMuted) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = Teal)
            Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

private fun greeting(name: String): String {
    val hour = LocalTime.now().hour
    val part = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (name.isBlank()) part else "$part, $name"
}
