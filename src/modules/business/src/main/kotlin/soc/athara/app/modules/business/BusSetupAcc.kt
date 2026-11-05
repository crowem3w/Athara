package soc.athara.app.modules.business

import androidx.compose.runtime.Composable
import soc.athara.app.core.identityapi.AccountType
import soc.athara.app.shared.authentication.ui.AccountSetupScreen


@Composable
internal fun BusSetupAcc(onBack: () -> Unit) {
    AccountSetupScreen(accountType = AccountType.BUSINESS, onBack = onBack)
}