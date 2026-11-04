package org.example.shared.authentication.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.core.identityapi.AccountType

/**
 * Sign-up form for one [AccountType]. Public entry point for the account-type
 * modules (:personal, :business, :organization); the account is created with
 * [accountType] and the user is signed in on success.
 *
 * @param onSignedUp called once the account has been created and the user is signed in.
 * @param onSignIn   called when the user taps "Already have an account? Sign in".
 * @param onBack     called when the user taps the back button (return to account type selection).
 */
@Composable
fun AccountSignUp(
    accountType: AccountType,
    onSignedUp: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
) {
    val vm: AuthViewModel = viewModel(factory = AuthViewModel.factory(AuthMode.SIGN_UP, accountType))
    val state by vm.state.collectAsState()
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) onSignedUp()
    }
    AuthenticationScreen(
        state = state,
        onMode = vm::setMode,
        onName = vm::onName,
        onFirstName = vm::onFirstName,
        onSurname = vm::onSurname,
        onUsername = vm::onUsername,
        onEmail = vm::onEmail,
        onLoginId = vm::onLoginId,
        birthday = vm.birthdayActions,
        onMobile = vm::onMobile,
        onCountry = vm::onCountry,
        onPassword = vm::onPassword,
        onConfirm = vm::onConfirm,
        onSubmit = vm::submit,
        onSocial = vm::signInWithSocial,
        onToSignUp = {},
        onToSignIn = onSignIn,
        onIndustry = vm::onIndustry,
        onPhone = vm::onPhone,
        onRegistrationId = vm::onRegistrationId,
        onBack = onBack,
        social = vm.socialActions,
    )
}
