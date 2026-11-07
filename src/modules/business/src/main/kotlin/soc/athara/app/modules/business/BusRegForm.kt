package soc.athara.app.modules.business

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import soc.athara.app.core.identityapi.Identity
import soc.athara.app.core.identityapi.AccountType
import soc.athara.app.core.identityapi.AuthenticationContract
import soc.athara.app.shared.designsystem.AppTheme







class BusRegForm : ComponentActivity() {

    /** Login / sign-up for this account type, opened from the guest Home. Signed in: tell the app layer to route again. */
    private val authLauncher = registerForActivityResult(AuthenticationContract()) { signedIn ->
        if (signedIn) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user = Identity.repository(this).currentUser.value
        
        val setupPendingAtStart = user != null && Identity.isSetupPending(this)
        setContent {
            var setupPending by rememberSaveable { mutableStateOf(setupPendingAtStart) }
            AppTheme(AccountType.BUSINESS) {
                if (user != null && setupPending) {
                    BusSetupAcc(onBack = { finishSetup(); setupPending = false }) 
                } else {
                    BusinessHomeScreen(
                        user = user,
                        onSignOut = {
                            lifecycleScope.launch {
                                Identity.signOut(this@BusRegForm)
                                finish()
                            }
                        },
                        onLogin = { authLauncher.launch(AccountType.BUSINESS) },
                    )
                }
            }
        }
    }

    private fun finishSetup() = Identity.completeSetup(this)
}