package social.ahtara.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import social.ahtara.app.R

/** Modal "Account" dialog: Log in and Sign up buttons, stacked vertically. Tap outside to close. */
@Composable
fun AccountDialog(onLogin: () -> Unit, onSignUp: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.account_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.account_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.account_dialog_login))
                }
                OutlinedButton(onClick = onSignUp, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(R.string.account_dialog_sign_up))
                }
            }
        }
    }
}

/**
 * Gate for actions that need an account (Create, like, comment, share).
 * Calling it returns true when the action may go ahead; for a guest it returns false and the
 * host screen shows the [AccountDialog]. Defaults to "always allowed".
 */
val LocalRequireAccount = androidx.compose.runtime.staticCompositionLocalOf<() -> Boolean> { { true } }
