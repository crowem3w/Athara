package org.example.modules.organization

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
import org.example.core.identityapi.Identity
import org.example.core.identityapi.AccountType
import org.example.shared.authentication.ui.AccountSignUp
import org.example.shared.designsystem.AppTheme







class OrgRegForm : ComponentActivity() {

    
    private val viewModel: OrganizationViewModel by viewModels {
        OrganizationViewModel.factory(Identity.repository(this).currentUser.value?.id.orEmpty())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user = Identity.repository(this).currentUser.value
        
        val setupPendingAtStart = user != null && Identity.isSetupPending(this)
        setContent {
            var setupPending by rememberSaveable { mutableStateOf(setupPendingAtStart) }
            var setupStep by rememberSaveable { mutableStateOf(false) }
            AppTheme(AccountType.ORGANIZATION) {
                if (user != null && setupPending) {
                    OrgSetupAcc(onBack = { finishSetup(); setupPending = false }) 
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
                        onSignedUp = { setupStep = true }, 
                        onSignIn = {
                            setResult(Activity.RESULT_FIRST_USER)
                            finish()
                        },
                        onBack = { finish() }, 
                    )
                }
            }
        }
    }

    private fun finishSetup() = Identity.completeSetup(this)
}