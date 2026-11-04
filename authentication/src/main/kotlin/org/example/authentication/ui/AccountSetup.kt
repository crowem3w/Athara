package org.example.authentication.ui

import androidx.compose.runtime.Composable
import org.example.identityapi.AccountType

/**
 * Account setup step shown after registration (step 4 of the sign-up progress bar). Same layout as
 * the registration forms: type illustration on top, white panel with a fixed title below.
 * The panel is empty for now; [content] is where the setup fields will go.
 * Public entry point for PerSetupAcc, BusSetupAcc and OrgSetupAcc.
 */
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
