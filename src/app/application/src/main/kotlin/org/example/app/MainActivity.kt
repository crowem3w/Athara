package org.example.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import org.example.app.onboarding.Onboarding
import org.example.app.onboarding.OnboardingContract
import org.example.core.identityapi.Identity
import org.example.shared.authentication.AccountTypeContract

/**
 * Launcher and router; it draws no UI of its own. Screen order:
 *
 *   splash (system) -> onboarding (first launch only) -> account type -> Home of that type (guest)
 *                                                                          \-> avatar -> login / sign-up -> Home (signed in)
 *
 * A signed-in user skips straight to the Home of their own account type.
 *
 * Result contract with the account activities (PerRegForm, BusRegForm, OrgRegForm):
 * RESULT_OK = the session changed (signed in from the guest Home), so route again;
 * anything else = the user left that screen.
 */
class MainActivity : ComponentActivity() {

    private val onboardingLauncher = registerForActivityResult(OnboardingContract()) { completed ->
        if (completed) route() else finish()
    }

    private val accountTypeLauncher = registerForActivityResult(AccountTypeContract()) { type ->
        if (type != null) homeLauncher.launch(type.intent(this)) else finish()
    }

    private val homeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when {
            result.resultCode == Activity.RESULT_OK -> route() // signed in from the guest Home
            Identity.isSignedIn(this) -> finish() // a signed-in user backed out of their Home
            else -> route() // guest backed out, or signed out: back to account type
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
