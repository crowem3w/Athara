package social.ahtara.authentication.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import social.ahtara.authentication.Authentication
import social.ahtara.authentication.AuthEntry
import social.ahtara.authentication.R
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.ui.theme.AuthenticationTheme










class AuthenticationActivity : ComponentActivity() {

    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (savedInstanceState == null &&
            intent.getStringExtra(EXTRA_ENTRY) == AuthEntry.SIGN_UP.name
        ) startSignUp()
        setContent {
            AuthenticationTheme {
                val state by viewModel.state.collectAsState()
                LaunchedEffect(state.signedIn) {
                    if (state.signedIn) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }
                
                
                BackHandler(enabled = state.mode != AuthMode.SIGN_IN) {
                    viewModel.setMode(AuthMode.SIGN_IN, null)
                }
                Box(Modifier.fillMaxSize()) {
                when (state.mode) {
                    AuthMode.SIGN_IN -> LoginScreen(
                        state = state,
                        onPassword = viewModel::onPassword,
                        onSubmit = viewModel::submit,
                        onLoginId = viewModel::onLoginId,
                        onForgotPassword = { viewModel.setMode(AuthMode.FORGOT_PASSWORD, null) },
                        onSocial = viewModel::signInWithSocial,
                        onSignUp = { startSignUp() },
                        social = viewModel.loginSocialActions,
                    )
                    
                    AuthMode.SELECT_ACCOUNT, AuthMode.SIGN_UP -> SignUpFlowScreen(
                        state = state,
                        onBack = {
                            viewModel.setMode(AuthMode.SIGN_IN, null)
                        },
                        onSelectType = viewModel::selectAccountType,
                        onSignIn = { viewModel.setMode(AuthMode.SIGN_IN, null) },
                        onMode = viewModel::setMode,
                        onName = viewModel::onName,
                        onFirstName = viewModel::onFirstName,
                        birthday = viewModel.birthdayActions,
                        onSurname = viewModel::onSurname,
                        onUsername = viewModel::onUsername,
                        onEmail = viewModel::onEmail,
                        onPassword = viewModel::onPassword,
                        onConfirm = viewModel::onConfirm,
                        onSubmit = viewModel::submit,
                        onSocial = viewModel::signInWithSocial,
                        onToSignUp = { startSignUp() },
                        onToSignIn = { viewModel.setMode(AuthMode.SIGN_IN, null) },
                        onIndustry = viewModel::onIndustry,
                        onPhone = viewModel::onPhone,
                        onRegistrationId = viewModel::onRegistrationId,
                        onMobile = viewModel::onMobile,
                        onCountry = viewModel::onCountry,
                        social = viewModel.socialActions,
                    )
                    
                    AuthMode.FORGOT_PASSWORD -> AuthenticationScreen(
                        state = state,
                        onMode = viewModel::setMode,
                        onName = viewModel::onName,
                        onLoginId = viewModel::onLoginId,
                        onPassword = viewModel::onPassword,
                        onConfirm = viewModel::onConfirm,
                        onSubmit = viewModel::submit,
                        onSocial = viewModel::signInWithSocial,
                        onToSignUp = { startSignUp() },
                        onToSignIn = { viewModel.setMode(AuthMode.SIGN_IN, null) },
                    )
                }
                
                if (state.mode == AuthMode.SIGN_IN) {
                    IconButton(
                        onClick = { finish() },
                        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.authentication_ic_close),
                            contentDescription = stringResource(R.string.authentication_action_close),
                        )
                    }
                }
                }
            }
        }
    }

    private fun startSignUp() =
        viewModel.selectAccountType(Authentication.selectedAccountType(this) ?: AccountType.PERSONAL)

    companion object {
        const val EXTRA_ENTRY = "social.ahtara.authentication.ENTRY"
    }
}