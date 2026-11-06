package soc.athara.app.modules.personal.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import soc.athara.app.modules.personal.R
import soc.athara.app.modules.personal.wallet.formatMoney
import soc.athara.app.shared.payments.PaymentError

private val SuccessGreen = Color(0xFF2E9E5B)

/** Tells the payer how the payment ended: a green check when it completed, a short message otherwise. */
@Composable
internal fun PaymentResultDialog(outcome: PayOutcome, onDone: () -> Unit) {
    when (outcome) {
        is PayOutcome.Completed -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text(stringResource(R.string.personal_pay_complete_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape).background(SuccessGreen),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = formatMoney(outcome.amountMinor),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.personal_pay_complete_body), style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.personal_receive_done)) } },
        )

        else -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text(stringResource(titleFor(outcome))) },
            text = { Text(stringResource(messageFor(outcome)), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.personal_receive_done)) } },
        )
    }
}

private fun titleFor(outcome: PayOutcome): Int = when (outcome) {
    PayOutcome.Cancelled -> R.string.personal_pay_cancelled_title
    PayOutcome.Expired -> R.string.personal_pay_expired_title
    PayOutcome.NoBusinessFound -> R.string.personal_pay_nobiz_title
    else -> R.string.personal_pay_failed_title
}

private fun messageFor(outcome: PayOutcome): Int = when (outcome) {
    PayOutcome.Cancelled -> R.string.personal_pay_cancelled_body
    PayOutcome.Expired -> R.string.personal_pay_expired_body
    PayOutcome.NoBusinessFound -> R.string.personal_pay_nobiz_body
    is PayOutcome.Failed -> when (outcome.error) {
        PaymentError.AMOUNT_OVER_LIMIT -> R.string.personal_pay_err_over_limit
        PaymentError.DAILY_LIMIT -> R.string.personal_pay_err_daily
        PaymentError.INSUFFICIENT_FUNDS -> R.string.personal_pay_err_funds
        PaymentError.NETWORK -> R.string.personal_pay_err_network
        PaymentError.RATE_LIMITED -> R.string.personal_pay_err_rate
        else -> R.string.personal_pay_err_other
    }
    is PayOutcome.Completed -> R.string.personal_pay_err_other
}
