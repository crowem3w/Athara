package org.example.organization

import androidx.compose.runtime.Composable
import org.example.identityapi.AccountType
import org.example.authentication.ui.AccountSetupScreen

/** Organization account setup: the step after OrgRegForm's sign-up form. Panel is empty for now. */
@Composable
fun OrgSetupAcc(onBack: () -> Unit) {
    AccountSetupScreen(accountType = AccountType.ORGANIZATION, onBack = onBack)
}
