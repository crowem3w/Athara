package org.example.personal

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.example.authentication.Authentication
import org.example.authentication.domain.AccountType
import org.example.authentication.ui.AccountSignUp
import org.example.authentication.ui.theme.AuthenticationTheme

/**
 * Personal account entry point.
 *  - Signed out: shows the personal sign-up form. Finishes with RESULT_OK once the
 *    account exists (RESULT_FIRST_USER = "I already have an account").
 *  - Signed in: shows the personal home screen; signing out finishes the Activity.
 */
class PerRegForm : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user = Authentication.repository(this).currentUser.value
        // Just created the account (sign-up happens in AuthenticationActivity): show setup before home.
        val setupPendingAtStart = user != null && Authentication.isSetupPending(this)
        setContent {
            var setupPending by rememberSaveable { mutableStateOf(setupPendingAtStart) }
            var setupStep by rememberSaveable { mutableStateOf(false) }
            // Latest user: the setup step saves picture paths on it after `user` was read
            val latestUser by Authentication.repository(this@PerRegForm).currentUser.collectAsState()
            AuthenticationTheme(AccountType.PERSONAL) {
                if (user != null && setupPending) {
                    PerSetupAcc(
                        onContinue = { setup ->
                            lifecycleScope.launch {
                                Authentication.savePersonalSetup(this@PerRegForm, setup)
                                finishSetup()
                                setupPending = false
                            }
                        },
                        onSkip = { finishSetup(); setupPending = false },
                        // Back to sign-up: delete the just-created account and sign out;
                        // MainActivity then re-opens authentication.
                        onBack = {
                            lifecycleScope.launch {
                                Authentication.discardNewAccount(this@PerRegForm)
                                finish()
                            }
                        },
                    )
                } else if (user != null) {
                    PersonalHomeScreen(
                        user = latestUser ?: user,
                        onSignOut = {
                            lifecycleScope.launch {
                                Authentication.signOut(this@PerRegForm)
                                finish()
                            }
                        },
                    )
                } else if (setupStep) {
                    PerSetupAcc(
                        onContinue = { setup ->
                            lifecycleScope.launch {
                                Authentication.savePersonalSetup(this@PerRegForm, setup)
                                finishSetup()
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        },
                        onSkip = {
                            finishSetup()
                            setResult(Activity.RESULT_OK)
                            finish()
                        },
                        // Back to the sign-up form: the just-created account is deleted.
                        onBack = {
                            lifecycleScope.launch {
                                Authentication.discardNewAccount(this@PerRegForm)
                                setupStep = false
                            }
                        },
                    )
                } else {
                    AccountSignUp(
                        accountType = AccountType.PERSONAL,
                        onSignedUp = { setupStep = true }, // next section: PerSetupAcc
                        onSignIn = {
                            setResult(Activity.RESULT_FIRST_USER)
                            finish()
                        },
                        onBack = { finish() }, // result stays CANCELED: back to account type selection
                    )
                }
            }
        }
    }

    private fun finishSetup() = Authentication.completeSetup(this)
}
