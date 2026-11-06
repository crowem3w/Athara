package soc.athara.app.modules.personal

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
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
import soc.athara.app.modules.personal.home.components.CreateSheet
import soc.athara.app.modules.personal.home.components.NfcCard
import soc.athara.app.modules.personal.home.components.PaymentReceivedDialog
import soc.athara.app.modules.personal.wallet.FakeBalanceStore
import soc.athara.app.modules.personal.wallet.SetBalanceDialog
import soc.athara.app.modules.personal.wallet.formatMoney
import soc.athara.app.shared.designsystem.theme.AtharaTheme

private val CategorySaver = listSaver<Set<PlaceCategory>, String>(
    save = { selected -> selected.map { it.name } },
    restore = { names -> names.mapNotNull { PlaceCategory.fromName(it) }.toSet() },
)

/**
 * Entry point of the Personal home. Owns the screen-level state (category filter, profile sheet, Send/Receive sheet, NFC card);
 * layout and feed behaviour live in [HomeScreen].
 */
@Composable
internal fun PersonalHomeScreen(user: AuthUser?, onSignOut: () -> Unit, onLogin: () -> Unit) {
    var categories by rememberSaveable(stateSaver = CategorySaver) { mutableStateOf(emptySet<PlaceCategory>()) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showSetBalance by rememberSaveable { mutableStateOf(false) }
    // > 0 while the NFC card is open (the amount being requested, in minor units).
    var cardMinor by rememberSaveable { mutableStateOf(0L) }
    // > 0 while the "payment received" dialog is showing.
    var receivedMinor by rememberSaveable { mutableStateOf(0L) }

    // Fake test balance (minor units), kept on this device until the server ledger exists.
    val context = LocalContext.current
    val balanceStore = remember { FakeBalanceStore(context) }
    var balanceMinor by remember(user?.id) {
        mutableStateOf(user?.let { balanceStore.get(it.id) } ?: 0L)
    }

    AtharaTheme {
        // Blur the home screen while the card is open (no-op below Android 12; the dim layer still applies).
        val blurRadius by animateDpAsState(if (cardMinor > 0) 18.dp else 0.dp, label = "homeBlur")
        Box {
            Box(modifier = Modifier.blur(blurRadius)) {
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
                    // Centre button = Send / Receive sheet. Guests have no account to pay from or into, so it sends them to login.
                    onCreateClick = { if (user == null) onLogin() else showCreate = true },
                    // Guests can browse; likes and saves need an account and are ignored until they log in.
                    onRequireAccount = { user != null },
                )
            }

            if (cardMinor > 0 && user != null) {
                NfcCard(
                    onClose = { cardMinor = 0L },
                    onScanned = {
                        // Test only: credit the fake balance. The server ledger will own this later.
                        val updated = balanceMinor + cardMinor
                        balanceStore.set(user.id, updated)
                        balanceMinor = updated
                        receivedMinor = cardMinor
                        cardMinor = 0L
                    },
                )
            }
        }

        if (showCreate && user != null) {
            // Pay closes the sheet and opens the NFC card.
            CreateSheet(
                balanceMinor = balanceMinor,
                onDismiss = { showCreate = false },
                onPay = { minor ->
                    showCreate = false
                    cardMinor = minor
                },
            )
        }

        if (receivedMinor > 0) {
            PaymentReceivedDialog(amountMinor = receivedMinor, onDone = { receivedMinor = 0L })
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
