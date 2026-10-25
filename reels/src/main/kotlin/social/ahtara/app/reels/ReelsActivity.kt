package social.ahtara.app.reels

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import social.ahtara.app.ui.components.AccountDialog
import social.ahtara.app.ui.components.LocalRequireAccount
import social.ahtara.authentication.AuthEntry
import social.ahtara.authentication.Authentication
import social.ahtara.authentication.AuthenticationContract
import social.ahtara.app.reels.ui.ReelsScreen
import social.ahtara.app.ui.theme.Theme

class ReelsActivity : ComponentActivity() {
    private val authLauncher = registerForActivityResult(AuthenticationContract()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            Theme {
                val user by Authentication.repository(this@ReelsActivity).currentUser.collectAsState()
                var showAccountDialog by rememberSaveable { mutableStateOf(false) }
                val requireAccount: () -> Boolean = {
                    if (user != null) true else { showAccountDialog = true; false }
                }
                CompositionLocalProvider(LocalRequireAccount provides requireAccount) {
                    ReelsScreen(reels = remember { loadReels() }, onBack = { finish() })
                }
                if (showAccountDialog) {
                    AccountDialog(
                        onLogin = { showAccountDialog = false; authLauncher.launch(AuthEntry.SIGN_IN) },
                        onSignUp = { showAccountDialog = false; authLauncher.launch(AuthEntry.SIGN_UP) },
                        onDismiss = { showAccountDialog = false },
                    )
                }
            }
        }
    }
}