package soc.athara.app.shared.authentication.ui

import androidx.compose.runtime.Composable
import soc.athara.app.core.identityapi.AccountType







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