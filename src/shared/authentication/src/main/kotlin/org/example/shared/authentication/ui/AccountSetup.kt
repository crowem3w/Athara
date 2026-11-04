package org.example.shared.authentication.ui

import androidx.compose.runtime.Composable
import org.example.core.identityapi.AccountType







@Composable
fun AccountSetupScreen(
    accountType: AccountType,
    onBack: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {},
) {
    RegistrationScreen(
        accountType = accountType,
        title = accountType.setupTitle,
        onBack = onBack,
        currentStep = STEP_SETUP,
        content = content,
    )
}