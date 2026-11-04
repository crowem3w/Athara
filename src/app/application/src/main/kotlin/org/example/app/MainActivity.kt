package org.example.app

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
import org.example.shared.authentication.AuthenticationContract
import org.example.core.identityapi.Identity
import org.example.app.onboarding.Onboarding
import org.example.app.onboarding.OnboardingContract
import org.example.app.ui.theme.TestTheme

class MainActivity : ComponentActivity() {
    private var authInProgress = false
    private var homeInProgress = false

    
    private val authLauncher = registerForActivityResult(AuthenticationContract()) { signedIn ->
        authInProgress = false
        if (!signedIn) finish()
    }

    
    
    
    private val homeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        homeInProgress = false
        if (Identity.isSignedIn(this)) finish()
    }

    
    
    private val onboardingLauncher = registerForActivityResult(OnboardingContract()) { completed ->
        if (!completed) finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (savedInstanceState == null && !Onboarding.isCompleted(this)) {
            onboardingLauncher.launch(Unit)
        }
        
        
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Identity.repository(this@MainActivity).currentUser.collect { user ->
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