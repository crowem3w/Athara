package org.example.modules.personal

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
import org.example.core.identityapi.AccountType
import org.example.core.identityapi.PersonalSetup
import org.example.shared.authentication.ui.AccountSetupScreen
import org.example.shared.authentication.ui.PersonalSetupFields








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