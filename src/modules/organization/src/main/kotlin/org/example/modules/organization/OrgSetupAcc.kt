package org.example.modules.organization

import androidx.compose.runtime.Composable
import org.example.core.identityapi.AccountType
import org.example.shared.authentication.ui.AccountSetupScreen


@Composable
internal fun OrgSetupAcc(onBack: () -> Unit) {
    AccountSetupScreen(accountType = AccountType.ORGANIZATION, onBack = onBack)
}