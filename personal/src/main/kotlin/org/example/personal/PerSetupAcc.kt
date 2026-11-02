package org.example.personal

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.example.authentication.domain.AccountType
import org.example.authentication.domain.PersonalSetup
import org.example.authentication.ui.AccountSetupScreen
import org.example.authentication.ui.PersonalSetupFields

/**
 * Personal account setup: the step after PerRegForm's sign-up form. Collects profile picture,
 * about and location (all optional). Continue saves them on the user; Skip for now saves nothing.
 *
 * Going back (header arrow or system back) goes back to sign-up and the new account is deleted,
 * so it first asks for confirmation. [onBack] runs only once the user has confirmed.
 */
@Composable
fun PerSetupAcc(
    onContinue: (PersonalSetup) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmBack by rememberSaveable { mutableStateOf(false) }
    BackHandler { confirmBack = true }

    AccountSetupScreen(accountType = AccountType.PERSONAL, onBack = { confirmBack = true }) {
        PersonalSetupFields(onContinue = onContinue, onSkip = onSkip)
    }

    if (confirmBack) {
        AlertDialog(
            onDismissRequest = { confirmBack = false },
            title = { Text(stringResource(R.string.personal_setup_back_title)) },
            text = { Text(stringResource(R.string.personal_setup_back_message)) },
            confirmButton = {
                TextButton(onClick = { confirmBack = false; onBack() }) {
                    Text(stringResource(R.string.personal_setup_back_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBack = false }) {
                    Text(stringResource(R.string.personal_setup_back_stay))
                }
            },
        )
    }
}
