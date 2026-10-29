package social.ahtara.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import social.ahtara.app.ui.components.BottomNavBar
import social.ahtara.app.ui.components.BusinessTab
import social.ahtara.app.ui.components.BusinessTabs
import social.ahtara.app.ui.components.TopBar

/**
 * Home for Business accounts, based on Personal's [HomeScreen] with the travel-feed parts removed:
 * no feed, no collapsing header / pull-to-refresh, no destination search, no travel category filters,
 * and no Transport / Places tabs in the bottom bar. What remains is the top bar, placeholder
 * [BusinessTabs], the Message tab and a QR scanner button (replacing Create), with an empty body to fill in later.
 *
 * The QR scanner is a button only: [onScanClick] is not wired yet and what a scan does is undecided.
 * It exists only on this screen, i.e. only for signed-in Business accounts. When you implement it, also
 * require `!Authentication.isSetupPending(context)` so it is unavailable while setup is unfinished.
 */
@Composable
fun BusinessHomeScreen(
    userInitial: String = "B",
    onProfileClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
) {
    var tabName by rememberSaveable { mutableStateOf(BusinessTab.OVERVIEW.name) }
    val tab = BusinessTab.entries.firstOrNull { it.name == tabName } ?: BusinessTab.OVERVIEW

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                onCreateClick = onScanClick,
                showTransport = false,
                showPlaces = false,
                centerIcon = Icons.Filled.QrCodeScanner,
                centerLabel = "Scan QR code",
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            TopBar(onProfileClick = onProfileClick, userInitial = userInitial)
            Spacer(Modifier.height(12.dp))
            BusinessTabs(selected = tab, onSelect = { tabName = it.name })
            Spacer(Modifier.height(8.dp))
            // Empty body: the selected tab's content goes here.
            Box(Modifier.fillMaxWidth().weight(1f))
        }
    }
}
