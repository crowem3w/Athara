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
            
            Box(Modifier.fillMaxWidth().weight(1f))
        }
    }
}