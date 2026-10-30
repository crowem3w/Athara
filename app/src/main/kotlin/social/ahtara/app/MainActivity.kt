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
import social.ahtara.location.PickLocationContract
import social.ahtara.app.ui.theme.Theme
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.Authentication
import social.ahtara.app.accounttype.AccountTypeStep
import social.ahtara.app.onboarding.Onboarding
import social.ahtara.app.onboarding.OnboardingContract
import social.ahtara.app.onboarding.ui.TermsDialog











class MainActivity : ComponentActivity() {
    private var feedInProgress = false
    private var onboardingDone by mutableStateOf(false)
    private var termsAccepted by mutableStateOf(false)
    private var accountTypeChosen by mutableStateOf(false)
    private var locationChosen by mutableStateOf(false)
    private var locationInProgress = false

    private val feedLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        
        feedInProgress = false
        finish()
    }

    private val locationLauncher = registerForActivityResult(PickLocationContract(preHome = true)) { picked ->
        locationInProgress = false
        if (picked != null) {
            if (picked.isNotEmpty()) Authentication.saveDevicePlaces(this, picked)
            Authentication.markLocationStepDone(this) 
            locationChosen = true
            openFeedIfReady()
        } else if (Authentication.needsAccountTypeStep(this) || AccountType.available.size > 1) {
            accountTypeChosen = false 
        } else {
            finish()
        }
    }

    private val onboardingLauncher = registerForActivityResult(OnboardingContract()) { completed ->
        if (completed) {
            onboardingDone = true
            openLocationIfReady()
            openFeedIfReady() 
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
        locationChosen = !Authentication.needsLocationStep(this)
        locationInProgress = savedInstanceState?.getBoolean(STATE_LOCATION_IN_PROGRESS) ?: false

        
        if (savedInstanceState == null && !onboardingDone) {
            onboardingLauncher.launch(Unit)
        } else {
            openLocationIfReady()
            openFeedIfReady()
        }

        setContent {
            Theme {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                
                if (onboardingDone && termsAccepted && !accountTypeChosen) {
                    AccountTypeStep(
                        onSelected = { type ->
                            Authentication.selectAccountType(this@MainActivity, type)
                            accountTypeChosen = true
                            openLocationIfReady()
                            openFeedIfReady()
                        },
                        onBack = { finish() },
                    )
                }
                
                if (onboardingDone && !termsAccepted) {
                    TermsDialog(
                        onAccept = {
                            Onboarding.acceptTerms(this@MainActivity)
                            termsAccepted = true
                            openLocationIfReady() 
                            openFeedIfReady()
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
        outState.putBoolean(STATE_LOCATION_IN_PROGRESS, locationInProgress)
    }

    
    private fun openLocationIfReady() {
        if (locationInProgress || feedInProgress || locationChosen) return
        if (!onboardingDone || !termsAccepted || !accountTypeChosen) return
        locationInProgress = true
        locationLauncher.launch(emptyList())
    }

    
    private fun openFeedIfReady() {
        if (feedInProgress || !onboardingDone || !termsAccepted || !accountTypeChosen || !locationChosen) return
        feedInProgress = true
        feedLauncher.launch(SocialFeedActivity.intent(this))
    }

    private companion object {
        const val STATE_FEED_IN_PROGRESS = "feedInProgress"
        const val STATE_LOCATION_IN_PROGRESS = "locationInProgress"
    }
}