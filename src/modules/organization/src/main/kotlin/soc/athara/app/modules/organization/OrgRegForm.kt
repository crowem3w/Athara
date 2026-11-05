package soc.athara.app.modules.organization

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
import soc.athara.app.core.identityapi.Identity
import soc.athara.app.core.identityapi.AccountType
import soc.athara.app.core.identityapi.AuthenticationContract
import soc.athara.app.shared.designsystem.AppTheme







class OrgRegForm : ComponentActivity() {

    /** Login / sign-up for this account type, opened from the guest Home. Signed in: tell the app layer to route again. */
    private val authLauncher = registerForActivityResult(AuthenticationContract()) { signedIn ->
        if (signedIn) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    
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
                } else {
                    OrganizationGuestHome(onLogin = { authLauncher.launch(AccountType.ORGANIZATION) })
                }
            }
        }
    }

    private fun finishSetup() = Identity.completeSetup(this)
}