package soc.athara.app.modules.personal

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import soc.athara.app.modules.personal.features.travel.model.PlaceCategory
import soc.athara.app.modules.personal.features.travel.model.ServiceArea
import soc.athara.app.core.identityapi.AuthUser
import soc.athara.app.modules.personal.home.HomeScreen
import soc.athara.app.modules.personal.home.components.ReceivePaymentDialog
import soc.athara.app.modules.personal.wallet.FakeBalanceStore
import soc.athara.app.modules.personal.wallet.SetBalanceDialog
import soc.athara.app.modules.personal.wallet.formatMoney
import soc.athara.app.shared.designsystem.theme.AhtaraTheme

private val CategorySaver = listSaver<Set<PlaceCategory>, String>(
    save = { selected -> selected.map { it.name } },
    restore = { names -> names.mapNotNull { PlaceCategory.fromName(it) }.toSet() },
)

/**
 * Entry point of the Personal home. Owns the screen-level state (category filter, profile sheet);
 * layout and feed behaviour live in [HomeScreen].
 */
@Composable
internal fun PersonalHomeScreen(user: AuthUser?, onSignOut: () -> Unit, onLogin: () -> Unit) {
    var categories by rememberSaveable(stateSaver = CategorySaver) { mutableStateOf(emptySet<PlaceCategory>()) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showReceive by rememberSaveable { mutableStateOf(false) }
    var showSetBalance by rememberSaveable { mutableStateOf(false) }

    // Fake test balance (minor units), kept on this device until the server ledger exists.
    val context = LocalContext.current
    val balanceStore = remember { FakeBalanceStore(context) }
    var balanceMinor by remember(user?.id) {
        mutableStateOf(user?.let { balanceStore.get(it.id) } ?: 0L)
    }

    AhtaraTheme {
        HomeScreen(
            // Default service area until place picking exists; use emptyList() to see the "add a place" state.
            places = listOf(ServiceArea.default),
            categories = categories,
            onToggleCategory = { category ->
                categories = if (category in categories) categories - category else categories + category
            },
            onClearCategories = { categories = emptySet() },
            userInitial = user?.let { it.displayName.trim().firstOrNull()?.uppercase() ?: "?" },
            // Guest: the avatar opens login / sign-up. Signed in: it opens the profile dialog.
            onProfileClick = { if (user == null) onLogin() else showProfile = true },
            // Centre button = receive a payment. Guests have no account to be paid into, so it sends them to login.
            onCreateClick = { if (user == null) onLogin() else showReceive = true },
            // Guests can browse; likes and saves need an account and are ignored until they log in.
            onRequireAccount = { user != null },
        )

        if (showReceive && user != null) {
            ReceivePaymentDialog(
                onDismiss = { showReceive = false },
                onPaid = { minor ->
                    // Test only: credit the fake balance. The server ledger will own this later.
                    val updated = balanceMinor + minor
                    balanceStore.set(user.id, updated)
                    balanceMinor = updated
                },
            )
        }

        if (showProfile && user != null) {
            AlertDialog(
                onDismissRequest = { showProfile = false },
                title = { Text(user.displayName) },
                text = {
                    Column {
                        Text(stringResource(R.string.personal_profile_login, user.identifier))
                        Text(stringResource(R.string.personal_balance_line, formatMoney(balanceMinor)))
                        TextButton(onClick = { showProfile = false; showSetBalance = true }) {
                            Text(stringResource(R.string.personal_balance_set))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProfile = false; onSignOut() }) {
                        Text(stringResource(R.string.personal_action_sign_out))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showProfile = false }) {
                        Text(stringResource(R.string.personal_profile_close))
                    }
                },
            )
        }

        if (showSetBalance && user != null) {
            SetBalanceDialog(
                currentMinor = balanceMinor,
                onSave = { minor ->
                    balanceStore.set(user.id, minor)
                    balanceMinor = minor
                    showSetBalance = false
                },
                onDismiss = { showSetBalance = false },
            )
        }
    }
}
