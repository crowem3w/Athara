package social.ahtara.authentication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.ui.theme.AuthenticationTheme

/**
 * Pre-Home "Account type" page (shown once, after the Terms & Conditions).
 * It is one step of the existing sign-up progress bar (STEP_ACCOUNT_TYPE), same header and roller.
 */
@Composable
fun AccountTypeStep(onSelected: (AccountType) -> Unit, onBack: () -> Unit) {
    AuthenticationTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.statusBarsPadding().navigationBarsPadding()) {
                SignUpStepHeader(
                    currentStep = STEP_ACCOUNT_TYPE,
                    onBack = onBack,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp),
                )
                Column(modifier = Modifier.padding(horizontal = 24.dp).weight(1f)) {
                    AccountTypeScreen(onSelect = onSelected, onSignIn = null)
                }
            }
        }
    }
}
