package social.ahtara.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import social.ahtara.authentication.AuthEntry
import social.ahtara.authentication.Authentication
import social.ahtara.authentication.AuthenticationContract
import social.ahtara.authentication.ui.theme.AuthenticationTheme








class AccountActivity : ComponentActivity() {

    private val authLauncher = registerForActivityResult(AuthenticationContract()) { signedIn ->
        
        if (signedIn && Authentication.isSetupPending(this)) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val user by Authentication.repository(this@AccountActivity).currentUser.collectAsState()
            val current = user
            if (current != null) {
                AuthenticationTheme(current.accountType) {
                    AccountScreen(
                        user = current,
                        onBack = { finish() },
                        onSignOut = {
                            lifecycleScope.launch {
                                Authentication.signOut(this@AccountActivity)
                                finish()
                            }
                        },
                    )
                }
            } else {
                
                AuthenticationTheme {
                    GuestAccountScreen(
                        onBack = { finish() },
                        onLogIn = { authLauncher.launch(AuthEntry.SIGN_IN) },
                        onSignUp = { authLauncher.launch(AuthEntry.SIGN_UP) },
                    )
                }
            }
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, AccountActivity::class.java)
    }
}