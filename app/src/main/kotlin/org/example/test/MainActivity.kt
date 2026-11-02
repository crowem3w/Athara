package org.example.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.example.authentication.Authentication
import org.example.authentication.AuthenticationContract
import org.example.onboarding.Onboarding
import org.example.onboarding.OnboardingContract
import org.example.test.ui.theme.TestTheme

class MainActivity : ComponentActivity() {
    private var authInProgress = false
    private var homeInProgress = false

    // If the user backs out of authentication without signing in, close the app.
    private val authLauncher = registerForActivityResult(AuthenticationContract()) { signedIn ->
        authInProgress = false
        if (!signedIn) finish()
    }

    // Home screen of the signed-in user's account type (:personal / :business / :organization).
    // Signing out closes it and the session observer below re-opens authentication;
    // backing out while still signed in closes the app.
    private val homeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        homeInProgress = false
        if (Authentication.isSignedIn(this)) finish()
    }

    // If the user backs out of onboarding without finishing it, close the app.
    // Once onboarding is done, the session observer below opens sign-in.
    private val onboardingLauncher = registerForActivityResult(OnboardingContract()) { completed ->
        if (!completed) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // savedInstanceState == null: don't relaunch after rotation/recreation.
        if (savedInstanceState == null && !Onboarding.isCompleted(this)) {
            onboardingLauncher.launch(Unit)
        }
        // Signed out (and onboarding done): open authentication, which starts on the login
        // screen ("Sign up" leads to account-type selection). Signed in: open the home for their account type.
        // Covers: right after onboarding, cold start, sign-up/sign-in, and signOut().
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Authentication.repository(this@MainActivity).currentUser.collect { user ->
                    if (!Onboarding.isCompleted(this@MainActivity)) return@collect
                    if (user == null) {
                        if (!authInProgress) {
                            authInProgress = true
                            authLauncher.launch(Unit)
                        }
                    } else if (!homeInProgress) {
                        homeInProgress = true
                        homeLauncher.launch(user.accountType.intent(this@MainActivity))
                    }
                }
            }
        }
        setContent {
            TestTheme {
                GreetingScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.hello_world))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingScreenPreview() {
    TestTheme {
        GreetingScreen()
    }
}
