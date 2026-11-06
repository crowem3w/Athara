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

private val SuccessGreen = Color(0xFF2E9E5B)

/** Shown after the payer's phone read the token and the amount was credited. */
@Composable
internal fun PaymentReceivedDialog(amountMinor: Long, onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.personal_receive_paid_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = formatMoney(amountMinor),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.personal_receive_paid_body), style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = {
            TextButton(onClick = onDone) { Text(stringResource(R.string.personal_receive_done)) }
        },
    )
}
