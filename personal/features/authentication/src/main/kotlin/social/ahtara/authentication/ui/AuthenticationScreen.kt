package social.ahtara.authentication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.domain.Country
import social.ahtara.authentication.domain.SocialProvider

@Composable
internal fun AuthenticationScreen(
    state: AuthUiState,
    onMode: (AuthMode, Int?) -> Unit,
    onName: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmit: () -> Unit,
    onSocial: (SocialProvider) -> Unit,
    onToSignUp: () -> Unit,
    onToSignIn: () -> Unit,
    onIndustry: (String) -> Unit = {},
    onPhone: (String) -> Unit = {},
    onRegistrationId: (String) -> Unit = {},
    onFirstName: (String) -> Unit = {},
    onSurname: (String) -> Unit = {},
    onUsername: (String) -> Unit = {},
    onEmail: (String) -> Unit = {},
    onLoginId: (String) -> Unit = {},
    birthday: BirthdayActions = BirthdayActions.None,
    onMobile: (String) -> Unit = {},
    onCountry: (Country) -> Unit = {},
    social: SocialActions = SocialActions.None,
    onBack: (() -> Unit)? = null,
) {
    val mode = state.mode
    if (mode == AuthMode.SIGN_UP && onBack != null) {
        RegistrationScreen(accountType = state.accountType, title = registrationTitle(state), onBack = onBack) {
            AuthenticationFormContent(
                state = state,
                onMode = onMode,
                onName = onName,
                onPassword = onPassword,
                onConfirm = onConfirm,
                onSubmit = onSubmit,
                onSocial = onSocial,
                onToSignUp = onToSignUp,
                onToSignIn = onToSignIn,
                onIndustry = onIndustry,
                onPhone = onPhone,
                onRegistrationId = onRegistrationId,
                onFirstName = onFirstName,
                onSurname = onSurname,
                onUsername = onUsername,
                onEmail = onEmail,
                onLoginId = onLoginId,
                birthday = birthday,
                onMobile = onMobile,
                onCountry = onCountry,
                social = social,
                showTitle = false,
            )
        }
        return
    }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    
                    top = if (mode == AuthMode.SIGN_UP) 32.dp else 20.dp,
                    bottom = 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (mode == AuthMode.SIGN_UP && onBack != null) {
                SignUpStepHeader(currentStep = STEP_REGISTRATION, onBack = onBack)
                Spacer(Modifier.height(4.dp)) 
            }
            AuthenticationFormContent(
                state = state,
                onMode = onMode,
                onName = onName,
                onPassword = onPassword,
                onConfirm = onConfirm,
                onSubmit = onSubmit,
                onSocial = onSocial,
                onToSignUp = onToSignUp,
                onToSignIn = onToSignIn,
                onIndustry = onIndustry,
                onPhone = onPhone,
                onRegistrationId = onRegistrationId,
                onFirstName = onFirstName,
                onSurname = onSurname,
                onUsername = onUsername,
                onEmail = onEmail,
                onLoginId = onLoginId,
                birthday = birthday,
                onMobile = onMobile,
                onCountry = onCountry,
                social = social,
            )
        }
    }
}
