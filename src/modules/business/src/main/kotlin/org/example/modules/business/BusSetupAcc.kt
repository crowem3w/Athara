package org.example.modules.business

import androidx.compose.runtime.Composable
import org.example.core.identityapi.AccountType
import org.example.shared.authentication.ui.AccountSetupScreen


@Composable
internal fun BusSetupAcc(onBack: () -> Unit) {
    AccountSetupScreen(accountType = AccountType.BUSINESS, onBack = onBack)
}