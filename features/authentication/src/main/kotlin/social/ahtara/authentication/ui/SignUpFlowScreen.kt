package social.ahtara.authentication.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.Country
import social.ahtara.authentication.domain.SocialProvider
import social.ahtara.authentication.ui.theme.AuthenticationTheme

@Composable
internal fun SignUpFlowScreen(
    state: AuthUiState,
    onBack: () -> Unit,
    onSelectType: (AccountType) -> Unit,
    onSignIn: () -> Unit,
    onMode: (AuthMode, Int?) -> Unit,
    onName: (String) -> Unit,
    onPassword: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onSubmit: () -> Unit,
    onSocial: (SocialProvider) -> Unit,
    onToSignUp: () -> Unit,
    onToSignIn: () -> Unit,
    onIndustry: (String) -> Unit,
    onPhone: (String) -> Unit,
    onRegistrationId: (String) -> Unit,
    onFirstName: (String) -> Unit = {},
    onSurname: (String) -> Unit = {},
    onUsername: (String) -> Unit = {},
    onEmail: (String) -> Unit = {},
    onLoginId: (String) -> Unit = {},
    birthday: BirthdayActions = BirthdayActions.None,
    onMobile: (String) -> Unit,
    onCountry: (Country) -> Unit,
    social: SocialActions = SocialActions.None,
) {
    val registering = state.mode == AuthMode.SIGN_UP

    
    
    var lastFormState by remember { mutableStateOf(state.copy(mode = AuthMode.SIGN_UP)) }
    if (registering) SideEffect { lastFormState = state }
    val formState = if (registering) state else lastFormState

    val background by animateColorAsState(
        targetValue = if (registering) {
            registrationTint(colorResource(formState.accountType.accentColor))
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(150),
        label = "signUpBackground",
    )

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Column(modifier = Modifier.statusBarsPadding().imePadding()) {
            SignUpStepHeader(
                currentStep = if (registering) STEP_REGISTRATION else STEP_ACCOUNT_TYPE,
                onBack = onBack,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp),
            )
            AnimatedContent(
                targetState = registering,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "signUpStep",
            ) { showForm ->
                if (showForm) {
                    AuthenticationTheme(formState.accountType) {
                        RegistrationBody(accountType = formState.accountType, title = registrationTitle(formState)) {
                            AuthenticationFormContent(
                                state = formState,
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
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp),
                    ) {
                        AccountTypeScreen(onSelect = onSelectType, onSignIn = onSignIn)
                    }
                }
            }
        }
    }
}
