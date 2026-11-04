package org.example.modules.personal

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
import org.example.core.identityapi.Identity
import org.example.core.identityapi.AccountType
import org.example.shared.authentication.ui.AccountSignUp
import org.example.shared.designsystem.AppTheme







class PerRegForm : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user = Identity.repository(this).currentUser.value
        
        val setupPendingAtStart = user != null && Identity.isSetupPending(this)
        setContent {
            var setupPending by rememberSaveable { mutableStateOf(setupPendingAtStart) }
            var setupStep by rememberSaveable { mutableStateOf(false) }
            
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
                } else if (user != null) {
                    PersonalHomeScreen(
                        user = latestUser ?: user,
                        onSignOut = {
                            lifecycleScope.launch {
                                Identity.signOut(this@PerRegForm)
                                finish()
                            }
                        },
                    )
                } else if (setupStep) {
                    PerSetupAcc(
                        onContinue = { setup ->
                            lifecycleScope.launch {
                                Identity.savePersonalSetup(this@PerRegForm, setup)
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
                        
                        onBack = {
                            lifecycleScope.launch {
                                Identity.discardNewAccount(this@PerRegForm)
                                setupStep = false
                            }
                        },
                    )
                } else {
                    AccountSignUp(
                        accountType = AccountType.PERSONAL,
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