package soc.athara.app.modules.personal

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
import soc.athara.app.core.identityapi.AccountType
import soc.athara.app.core.identityapi.Identity
import soc.athara.app.core.identityapi.AuthenticationContract
import soc.athara.app.shared.designsystem.AppTheme











class PerRegForm : ComponentActivity() {

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

            val latestUser by Identity.repository(this@PerRegForm).currentUser.collectAsState()
            AppTheme(AccountType.PERSONAL) {
                if (user != null && setupPending) {
                    PerSetupAcc(
                        onContinue = { setup ->
                            lifecycleScope.launch {
                                Identity.savePersonalSetup(this@PerRegForm, setup)
                                finishSetup()
                                setupPending = false
                            }
                        },
                        onSkip = { finishSetup(); setupPending = false },
                        onBack = {
                            lifecycleScope.launch {
                                Identity.discardNewAccount(this@PerRegForm)
                                finish()
                            }
                        },
                    )
                } else {
                    PersonalHomeScreen(
                        user = user?.let { latestUser ?: it },
                        onSignOut = {
                            lifecycleScope.launch {
                                Identity.signOut(this@PerRegForm)
                                finish()
                            }
                        },
                        onLogin = { authLauncher.launch(AccountType.PERSONAL) },
                    )
                }
            }
        }
    }

    private fun finishSetup() = Identity.completeSetup(this)
}
