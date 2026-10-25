package social.ahtara.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import social.ahtara.app.ui.theme.Theme
import social.ahtara.authentication.Authentication
import social.ahtara.authentication.ui.AccountTypeStep
import social.ahtara.onboarding.Onboarding
import social.ahtara.onboarding.OnboardingContract
import social.ahtara.onboarding.ui.TermsDialog

/**
 * App entry point. Startup sequence:
 *
 *   Splash -> Onboarding (first launch only) -> Terms & Conditions (modal, until accepted)
 *   -> Account type (one step of the sign-up progress bar; skipped while only Personal is offered)
 *   -> Home.
 *   Login / Registration is NOT forced: a small "Account" dialog opens from Home only when a
 *   guest taps Create / like / comment / share; the profile button opens the Account settings screen.
 */
class MainActivity : ComponentActivity() {
    private var feedInProgress = false
    private var onboardingDone by mutableStateOf(false)
    private var termsAccepted by mutableStateOf(false)
    private var accountTypeChosen by mutableStateOf(false)

    private val feedLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Home was closed (back press): leave the app.
        feedInProgress = false
        finish()
    }

    private val onboardingLauncher = registerForActivityResult(OnboardingContract()) { completed ->
        if (completed) {
            onboardingDone = true
            openFeedIfReady() // no-op until the Terms are accepted and the account type chosen
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        feedInProgress = savedInstanceState?.getBoolean(STATE_FEED_IN_PROGRESS) ?: false
        onboardingDone = Onboarding.isCompleted(this)
        termsAccepted = Onboarding.isTermsAccepted(this)
        accountTypeChosen = !Authentication.needsAccountTypeStep(this)

        // 1. Onboarding, only on the very first launch.
        if (savedInstanceState == null && !onboardingDone) {
            onboardingLauncher.launch(Unit)
        } else {
            openFeedIfReady()
        }

        setContent {
            Theme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                // 3. Account type step, after the Terms and before Home.
                if (onboardingDone && termsAccepted && !accountTypeChosen) {
                    AccountTypeStep(
                        onSelected = { type ->
                            Authentication.selectAccountType(this@MainActivity, type)
                            accountTypeChosen = true
                            openFeedIfReady()
                        },
                        onBack = { finish() },
                    )
                }
                // 2. Terms & Conditions modal, shown after onboarding until accepted.
                if (onboardingDone && !termsAccepted) {
                    TermsDialog(
                        onAccept = {
                            Onboarding.acceptTerms(this@MainActivity)
                            termsAccepted = true
                            openFeedIfReady() // no-op until the account type is chosen
                        },
                        onDecline = { finish() },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_FEED_IN_PROGRESS, feedInProgress)
    }

    /** 4. Home, once onboarding, the Terms and the account type are done. Works signed out (guest) or signed in. */
    private fun openFeedIfReady() {
        if (feedInProgress || !onboardingDone || !termsAccepted || !accountTypeChosen) return
        feedInProgress = true
        feedLauncher.launch(SocialFeedActivity.intent(this))
    }

    private companion object {
        const val STATE_FEED_IN_PROGRESS = "feedInProgress"
    }
}
