package soc.athara.app.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import soc.athara.app.app.onboarding.Onboarding
import soc.athara.app.app.onboarding.OnboardingContract
import soc.athara.app.core.identityapi.Identity
import soc.athara.app.shared.authentication.AccountTypeContract













class MainActivity : ComponentActivity() {

    private val onboardingLauncher = registerForActivityResult(OnboardingContract()) { completed ->
        if (completed) route() else finish()
    }

    private val accountTypeLauncher = registerForActivityResult(AccountTypeContract()) { type ->
        if (type != null) homeLauncher.launch(type.intent(this)) else finish()
    }

    private val homeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when {
            result.resultCode == Activity.RESULT_OK -> route() 
            Identity.isSignedIn(this) -> finish() 
            else -> route() 
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) route()
    }

    private fun route() {
        if (!Onboarding.isCompleted(this)) {
            onboardingLauncher.launch(Unit)
            return
        }
        val user = Identity.repository(this).currentUser.value
        if (user != null) homeLauncher.launch(user.accountType.intent(this)) else accountTypeLauncher.launch(Unit)
    }
}
