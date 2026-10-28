package social.ahtara.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import social.ahtara.account.AccountActivity
import social.ahtara.app.location.PickLocationContract
import social.ahtara.app.ui.screens.BlankHomeScreen
import social.ahtara.app.ui.screens.HomeScreen
import social.ahtara.app.ui.screens.PlacesScreen
import social.ahtara.app.ui.screens.feedKey
import social.ahtara.app.ui.theme.Theme
import social.ahtara.authentication.Authentication
import social.ahtara.app.ui.components.AccountDialog
import social.ahtara.app.ui.components.LocalRequireAccount
import social.ahtara.authentication.AuthEntry
import social.ahtara.authentication.AuthenticationContract
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.ServiceArea
import social.ahtara.travel.model.PlaceCategory











/**
 * Home screen. Works for guests (signed out, Guimaras default) and signed-in users.
 * A guest tapping Create, like, comment or share gets the "Account" dialog (Log in / Sign up, stacked vertically).
 * The profile button opens the Account settings screen (not a dialog), for guests too.
 */
class SocialFeedActivity : ComponentActivity() {
    private var setupInProgress = false

    // Login / Sign-up screens, opened from the "Account" dialog. A new account's setup is
    // started by the observer below.
    private val authLauncher = registerForActivityResult(AuthenticationContract()) { }

    private val setupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        setupInProgress = false
        // Setup abandoned: drop the half-created account and carry on as a guest.
        if (Authentication.isSignedIn(this) && Authentication.isSetupPending(this)) {
            lifecycleScope.launch { Authentication.discardNewAccount(this@SocialFeedActivity) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setupInProgress = savedInstanceState?.getBoolean(STATE_SETUP_IN_PROGRESS) ?: false
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                Authentication.repository(this@SocialFeedActivity).currentUser.collect { user ->
                    // Signed in with account setup still pending (fresh registration, or the app
                    // was closed mid-setup): continue into that account type's setup screens.
                    if (user != null && Authentication.isSetupPending(this@SocialFeedActivity) && !setupInProgress) {
                        setupInProgress = true
                        setupLauncher.launch(user.accountType.intent(this@SocialFeedActivity))
                    }
                }
            }
        }
        setContent {
            Theme {
                val user by Authentication.repository(this@SocialFeedActivity).currentUser.collectAsState()
                val current = user
                var showAccountDialog by rememberSaveable { mutableStateOf(false) }

                val pickPlaces = rememberLauncherForActivityResult(
                    PickLocationContract(current?.accountType ?: AccountType.PERSONAL),
                ) { picked ->
                    if (picked != null) {
                        lifecycleScope.launch { Authentication.savePlaces(this@SocialFeedActivity, picked) }
                    }
                }

                var showPlaces by rememberSaveable { mutableStateOf(false) }
                                var categoryNames by rememberSaveable { mutableStateOf(emptySet<String>()) }
                var activeKeys by rememberSaveable { mutableStateOf(emptySet<String>()) }
                val categories = remember(categoryNames) { categoryNames.mapNotNull { PlaceCategory.fromName(it) }.toSet() }

                // Only Guimaras is shown. Guests (and users with no Guimaras place saved) get the default.
                val guimarasPlaces = remember(current?.places) {
                    current?.places.orEmpty().filter { ServiceArea.contains(it) }
                }
                val basePlaces = guimarasPlaces.ifEmpty { GuestPlaces.forDevice(this@SocialFeedActivity) }
                val activePlaces = remember(basePlaces, activeKeys) {
                    if (activeKeys.isEmpty()) basePlaces
                    else basePlaces.filter { it.feedKey() in activeKeys }.ifEmpty { basePlaces }
                }
                val toggleCategory: (PlaceCategory) -> Unit = { c ->
                    categoryNames = if (c.name in categoryNames) categoryNames - c.name else categoryNames + c.name
                }
                val clearCategories: () -> Unit = { categoryNames = emptySet() }
                val togglePlace: (social.ahtara.authentication.domain.PlaceSelection) -> Unit = { place ->
                    val all = basePlaces.map { it.feedKey() }.toSet()
                    val ticked = if (activeKeys.isEmpty()) all else activeKeys.intersect(all)
                    val next = if (place.feedKey() in ticked) ticked - place.feedKey() else ticked + place.feedKey()
                    activeKeys = if (next.isEmpty() || next == all) emptySet() else next
                }

                // Gate shared with the feed's like / comment / share buttons.
                val requireAccount: () -> Boolean = {
                    if (current != null) true else { showAccountDialog = true; false }
                }
                CompositionLocalProvider(LocalRequireAccount provides requireAccount) {
                Box(Modifier.fillMaxSize()) {
                    // Business and Organization accounts: blank home (top bar + bottom nav) for now.
                    // Personal accounts and guests get the social feed.
                    if (current != null && current.accountType != AccountType.PERSONAL) {
                        BlankHomeScreen(
                            userInitial = current.displayName.trim().firstOrNull()?.uppercase() ?: "G",
                            onProfileClick = { startActivity(AccountActivity.intent(this@SocialFeedActivity)) },
                        )
                    } else HomeScreen(
                        places = activePlaces,
                        categories = categories,
                        onToggleCategory = toggleCategory,
                        onClearCategories = clearCategories,
                        userInitial = current?.displayName?.trim()?.firstOrNull()?.uppercase() ?: "G",
                        onPlacesClick = { showPlaces = true },
                        // Account settings is a full screen; guests see Log in / Sign up there.
                        onProfileClick = { startActivity(AccountActivity.intent(this@SocialFeedActivity)) },
                        // Only Create needs an account: guests get the "Account" dialog.
                        onCreateClick = { requireAccount() },
                    )
                    if (showAccountDialog) {
                        AccountDialog(
                            onLogin = { showAccountDialog = false; authLauncher.launch(AuthEntry.SIGN_IN) },
                            onSignUp = { showAccountDialog = false; authLauncher.launch(AuthEntry.SIGN_UP) },
                            onDismiss = { showAccountDialog = false },
                        )
                    }
                    if (showPlaces && (current == null || current.accountType == AccountType.PERSONAL)) {
                        BackHandler { showPlaces = false }
                        PlacesScreen(
                            places = if (current != null) guimarasPlaces else GuestPlaces.forDevice(this@SocialFeedActivity),
                            activeKeys = activeKeys,
                            categories = categories,
                            onTogglePlace = togglePlace,
                            onRemovePlace = { place ->
                                // Guests have nothing saved; only signed-in users edit places.
                                if (current != null) lifecycleScope.launch {
                                    Authentication.savePlaces(this@SocialFeedActivity, current.places - place)
                                }
                            },
                            onAddPlace = { if (current != null) pickPlaces.launch(guimarasPlaces) else showAccountDialog = true },
                            onToggleCategory = toggleCategory,
                            onClearCategories = clearCategories,
                            onBack = { showPlaces = false },
                        )
                    }
                }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SETUP_IN_PROGRESS, setupInProgress)
    }

    companion object {
        private const val STATE_SETUP_IN_PROGRESS = "setupInProgress"
        fun intent(context: Context): Intent = Intent(context, SocialFeedActivity::class.java)
    }
}
