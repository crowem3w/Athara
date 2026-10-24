package social.ahtara.business

import androidx.compose.runtime.Composable
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.ui.AccountSetupScreen


@Composable
fun BusSetupAcc(onBack: () -> Unit) {
    AccountSetupScreen(accountType = AccountType.BUSINESS, onBack = onBack)
}