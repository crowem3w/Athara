package org.example.organization

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.example.identityapi.Identity
import org.example.identityapi.AccountType
import org.example.authentication.ui.AccountSignUp
import org.example.designsystem.AppTheme

/**
 * Organization account entry point.
 *  - Signed out: shows the organization sign-up form. Finishes with RESULT_OK once the
 *    account exists (RESULT_FIRST_USER = "I already have an account").
 *  - Signed in: shows the organization home screen; signing out finishes the Activity.
 */
class OrgRegForm : ComponentActivity() {

    // Only used while signed in: all rows are scoped to the signed-in user's id.
    private val viewModel: OrganizationViewModel by viewModels {
        OrganizationViewModel.factory(Identity.repository(this).currentUser.value?.id.orEmpty())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user = Identity.repository(this).currentUser.value
        // Just created the account (sign-up happens in AuthenticationActivity): show setup before home.
        val setupPendingAtStart = user != null && Identity.isSetupPending(this)
        setContent {
            var setupPending by rememberSaveable { mutableStateOf(setupPendingAtStart) }
            var setupStep by rememberSaveable { mutableStateOf(false) }
            AppTheme(AccountType.ORGANIZATION) {
                if (user != null && setupPending) {
                    OrgSetupAcc(onBack = { finishSetup(); setupPending = false }) // empty for now: back skips to home
                } else if (user != null) {
                    val state by viewModel.state.collectAsState()
                    OrganizationHomeScreen(
                        user = user,
                        state = state,
                        onAddMember = viewModel::addMember,
                        onAddTeam = viewModel::addTeam,
                        onAddEvent = viewModel::addEvent,
                        onDeleteMember = viewModel::deleteMember,
                        onDeleteTeam = viewModel::deleteTeam,
                        onDeleteEvent = viewModel::deleteEvent,
                        onSignOut = {
                            lifecycleScope.launch {
                                Identity.signOut(this@OrgRegForm)
                                finish()
                            }
                        },
                    )
                } else if (setupStep) {
                    OrgSetupAcc(
                        onBack = {
                            finishSetup()
                            setResult(Activity.RESULT_OK)
                            finish()
                        },
                    )
                } else {
                    AccountSignUp(
                        accountType = AccountType.ORGANIZATION,
                        onSignedUp = { setupStep = true }, // next section: OrgSetupAcc
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

    private fun finishSetup() = Identity.completeSetup(this)
}
