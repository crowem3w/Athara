package soc.athara.app.modules.personal.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import soc.athara.app.modules.personal.R
import soc.athara.app.modules.personal.wallet.CurrencySymbol
import soc.athara.app.modules.personal.wallet.formatMoney
import soc.athara.app.modules.personal.wallet.sanitizeAmount
import soc.athara.app.modules.personal.wallet.toMinorUnits
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TealLight

/** Lifetime of a payment request, matching the 60 to 120 s session expiry in the project plan. */
private const val SessionSeconds = 120

/**
 * Receiver dialog opened by the centre button.
 *
 * Step 1: enter an amount (and optional note) and tap Request.
 * Step 2: "Ready to receive" with a countdown while the payer taps their phone.
 *         For now a "Simulate payment" button stands in for the payer.
 * Step 3: "Payment received".
 *
 * [onPaid] receives the amount in minor units once, when the payment completes.
 *
 * TODO: Request should call `POST /sessions` (amount, note) and use the returned `expires_at`,
 * session token and nonce; the HCE service should start serving that token here.
 * "Paid" should come from the server (push / sync), not from a button. Cancel should call
 * `POST /sessions/{id}/cancel`.
 */
@Composable
internal fun ReceivePaymentDialog(onDismiss: () -> Unit, onPaid: (Long) -> Unit) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    // 0 = still entering; > 0 = request is live (amount in minor units, e.g. centavos).
    var requestedMinor by rememberSaveable { mutableStateOf(0L) }
    var paid by rememberSaveable { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(SessionSeconds) }

    val live = requestedMinor > 0
    val expired = live && !paid && secondsLeft <= 0

    LaunchedEffect(requestedMinor) {
        if (requestedMinor > 0) {
            secondsLeft = SessionSeconds
            while (secondsLeft > 0 && !paid) {
                delay(1_000)
                secondsLeft--
            }
        }
    }

    when {
        !live -> {
            val minor = toMinorUnits(amountText)
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.personal_receive_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = sanitizeAmount(it) },
                            label = { Text(stringResource(R.string.personal_receive_amount_label)) },
                            prefix = { Text(CurrencySymbol) },
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(80) },
                            label = { Text(stringResource(R.string.personal_receive_note_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(enabled = minor > 0, onClick = { requestedMinor = minor }) {
                        Text(stringResource(R.string.personal_receive_request))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.personal_receive_cancel)) }
                },
            )
        }

        paid -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.personal_receive_paid_title)) },
                text = {
                    StatusBody(
                        icon = { Icon(Icons.Filled.Check, contentDescription = null, tint = Teal, modifier = Modifier.size(36.dp)) },
                        amount = formatMoney(requestedMinor),
                        message = stringResource(R.string.personal_receive_paid_body),
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.personal_receive_done)) }
                },
            )
        }

        else -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.personal_receive_ready_title)) },
                text = {
                    StatusBody(
                        icon = { Icon(Icons.Filled.TouchApp, contentDescription = null, tint = Teal, modifier = Modifier.size(36.dp)) },
                        amount = formatMoney(requestedMinor),
                        message = if (expired) stringResource(R.string.personal_receive_expired)
                        else stringResource(R.string.personal_receive_hold),
                        footnote = if (expired) null
                        else stringResource(R.string.personal_receive_expires, formatClock(secondsLeft)),
                    )
                },
                confirmButton = {
                    if (expired) {
                        TextButton(onClick = { requestedMinor = 0L }) {
                            Text(stringResource(R.string.personal_receive_new))
                        }
                    } else {
                        // Test stand-in for the payer's tap. Guarded so the balance can only be credited once.
                        TextButton(onClick = {
                            if (!paid) {
                                paid = true
                                onPaid(requestedMinor)
                            }
                        }) {
                            Text(stringResource(R.string.personal_receive_simulate))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.personal_receive_cancel_request))
                    }
                },
            )
        }
    }
}

@Composable
private fun StatusBody(
    icon: @Composable () -> Unit,
    amount: String,
    message: String,
    footnote: String? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TealLight),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.height(12.dp))
        Text(amount, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        if (footnote != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = footnote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatClock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
