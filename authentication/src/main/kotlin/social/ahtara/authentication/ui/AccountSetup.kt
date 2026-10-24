package social.ahtara.authentication.ui

import androidx.compose.runtime.Composable
import social.ahtara.authentication.domain.AccountType







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