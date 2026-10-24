package social.ahtara.personal

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import social.ahtara.authentication.Authentication
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.PersonalSetup
import social.ahtara.authentication.ui.AccountSignUp
import social.ahtara.authentication.ui.theme.AuthenticationTheme
import social.ahtara.app.location.PickLocationContract








class PerRegForm : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val user = Authentication.repository(this).currentUser.value
        
        val setupPendingAtStart = user != null && Authentication.isSetupPending(this)
        setContent {
            var setupPending by rememberSaveable { mutableStateOf(setupPendingAtStart) }
            var setupStep by rememberSaveable { mutableStateOf(false) }
            AuthenticationTheme(AccountType.PERSONAL) {
                if (user != null && setupPending) {
                    PerSetupWithLocation(
                        onSave = { setup ->
                            lifecycleScope.launch {
                                Authentication.savePersonalSetup(this@PerRegForm, setup)
                                finishSetup()
                                
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        },
                        
                        
                        onBack = {
                            lifecycleScope.launch {
                                Authentication.discardNewAccount(this@PerRegForm)
                                finish()
                            }
                        },
                    )
                } else if (user != null) {
                    
                    
                    LaunchedEffect(Unit) {
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } else if (setupStep) {
                    PerSetupWithLocation(
                        onSave = { setup ->
                            lifecycleScope.launch {
                                Authentication.savePersonalSetup(this@PerRegForm, setup)
                                finishSetup()
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        },
                        
                        onBack = {
                            lifecycleScope.launch {
                                Authentication.discardNewAccount(this@PerRegForm)
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

    private fun finishSetup() = Authentication.completeSetup(this)
}









@Composable
private fun PerSetupWithLocation(
    onSave: (PersonalSetup) -> Unit,
    onBack: () -> Unit,
) {
    
    var pending by rememberSaveable(stateSaver = PendingSetupSaver) { mutableStateOf<PersonalSetup?>(null) }

    val picker = rememberLauncherForActivityResult(PickLocationContract(AccountType.PERSONAL)) { picked ->
        val setup = pending
        pending = null
        if (picked != null && setup != null) {
            
            val main = picked.first()
            onSave(setup.copy(location = main.name, latitude = main.latitude, longitude = main.longitude, places = picked))
        }
    }

    PerSetupAcc(
        onContinue = { setup ->
            pending = setup
            picker.launch(emptyList())
        },
        onSkip = {
            pending = PersonalSetup() 
            picker.launch(emptyList())
        },
        onBack = onBack,
    )
}


private val PendingSetupSaver = listSaver<PersonalSetup?, Any?>(
    save = { s -> if (s == null) emptyList() else listOf(s.photoUri, s.coverUri, s.about) },
    restore = { l -> if (l.size == 3) PersonalSetup(photoUri = l[0] as String?, coverUri = l[1] as String?, about = l[2] as String) else null },
)