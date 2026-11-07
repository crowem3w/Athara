package soc.athara.app.modules.business.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import soc.athara.app.modules.business.R
import soc.athara.app.modules.business.model.peso
import soc.athara.app.modules.business.ui.AppCard
import soc.athara.app.modules.business.ui.IconRow
import soc.athara.app.modules.business.ui.PageTitle
import soc.athara.app.modules.business.ui.RowDivider
import soc.athara.app.modules.business.ui.SectionTitle
import soc.athara.app.modules.business.ui.StatusChip
import soc.athara.app.modules.business.ui.ToggleRow
import soc.athara.app.modules.business.ui.Tone
import soc.athara.app.shared.designsystem.theme.Background
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TextPrimary
import soc.athara.app.shared.designsystem.theme.TextSecondary

@Composable
internal fun AccountTab(
    businessName: String,
    identifier: String,
    listState: LazyListState,
    padding: PaddingValues,
    onMessage: (String) -> Unit,
    onSignOut: () -> Unit,
) {
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var bookingAlerts by rememberSaveable { mutableStateOf(true) }
    var orderAlerts by rememberSaveable { mutableStateOf(true) }
    var paymentAlerts by rememberSaveable { mutableStateOf(true) }
    var announcements by rememberSaveable { mutableStateOf(false) }
    val soon = { name: String -> onMessage("$name is coming in the next pass") }

    LazyColumn(state = listState, contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PageTitle("Account") }

        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Teal), contentAlignment = Alignment.Center) {
                        Text(businessName.trim().firstOrNull()?.uppercase() ?: "?", color = Background, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(businessName.ifBlank { "Your business" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(identifier, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    StatusChip("Business", Tone.Info)
                }
            }
        }

        item { SectionTitle("Earnings, last 30 days") }
        item {
            AppCard(onClick = { soon("Earnings details") }) {
                EarningsLine("Gross revenue", peso(61_800), bold = false)
                EarningsLine("Fees and refunds", "-" + peso(2_150), bold = false)
                RowDivider()
                EarningsLine("Net earnings", peso(59_650), bold = true)
            }
        }

        item {
            AppCard(padding = 0.dp) {
                IconRow(Icons.Outlined.History, "Transaction history", onClick = { soon("Transaction history") })
                RowDivider()
                IconRow(Icons.Outlined.Groups, "Staff", subtitle = "Who can use this business account", onClick = { soon("Staff management") })
                RowDivider()
                IconRow(Icons.AutoMirrored.Outlined.HelpOutline, "Help and support", onClick = { soon("Help and support") })
            }
        }

        item { SectionTitle("Notifications") }
        item {
            AppCard(padding = 0.dp) {
                ToggleRow("Bookings", bookingAlerts, { bookingAlerts = it })
                RowDivider()
                ToggleRow("Orders", orderAlerts, { orderAlerts = it })
                RowDivider()
                ToggleRow("Payments", paymentAlerts, { paymentAlerts = it })
                RowDivider()
                ToggleRow("Announcements", announcements, { announcements = it })
            }
        }

        item {
            AppCard(padding = 0.dp) {
                IconRow(
                    Icons.AutoMirrored.Outlined.Logout,
                    stringResource(R.string.business_action_sign_out),
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { confirmSignOut = true },
                )
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.business_action_sign_out) + "?") },
            text = { Text("You will need to log in again to collect payments.") },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; onSignOut() }) {
                    Text(stringResource(R.string.business_action_sign_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.business_action_cancel)) } },
        )
    }
}

@Composable
private fun EarningsLine(label: String, value: String, bold: Boolean) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (bold) TextPrimary else TextSecondary)
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = TextPrimary,
        )
    }
}
