package org.example.modules.business

import androidx.compose.runtime.Composable
import org.example.core.identityapi.AccountType
import org.example.shared.authentication.ui.AccountSetupScreen

/** Business account setup: the step after BusRegForm's sign-up form. Panel is empty for now. */
@Composable
fun BusSetupAcc(onBack: () -> Unit) {
    AccountSetupScreen(accountType = AccountType.BUSINESS, onBack = onBack)
}
