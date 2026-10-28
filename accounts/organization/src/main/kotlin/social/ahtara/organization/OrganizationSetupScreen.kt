package social.ahtara.organization

import androidx.compose.runtime.Composable
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.ui.AccountSetupScreen


@Composable
fun OrganizationSetupScreen(onBack: () -> Unit) {
    AccountSetupScreen(accountType = AccountType.ORGANIZATION, onBack = onBack)
}