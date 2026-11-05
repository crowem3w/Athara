package soc.athara.app.modules.organization

import androidx.compose.runtime.Composable
import soc.athara.app.core.identityapi.AccountType
import soc.athara.app.shared.authentication.ui.AccountSetupScreen


@Composable
internal fun OrgSetupAcc(onBack: () -> Unit) {
    AccountSetupScreen(accountType = AccountType.ORGANIZATION, onBack = onBack)
}