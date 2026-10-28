package social.ahtara.organization

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import social.ahtara.authentication.Authentication
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.ui.AccountSignUp
import social.ahtara.authentication.ui.theme.AuthenticationTheme








class OrganizationRegistrationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user = Authentication.repository(this).currentUser.value
        
        val setupPendingAtStart = user != null && Authentication.isSetupPending(this)
        setContent {
            var setupPending by rememberSaveable { mutableStateOf(setupPendingAtStart) }
            var setupStep by rememberSaveable { mutableStateOf(false) }
            AuthenticationTheme(AccountType.ORGANIZATION) {
                if (user != null && setupPending) {
                    OrganizationSetupScreen(onBack = { finishSetup(); setResult(Activity.RESULT_OK); finish() }) 
                } else if (user != null) {
                    
                    
                    LaunchedEffect(Unit) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } else if (setupStep) {
                    OrganizationSetupScreen(
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

    private fun finishSetup() = Authentication.completeSetup(this)
}