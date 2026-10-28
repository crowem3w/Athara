package social.ahtara.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import social.ahtara.app.ui.components.BottomNavBar
import social.ahtara.app.ui.components.TopBar

/**
 * Placeholder home for Business and Organization accounts: the top bar and bottom nav
 * with an empty body. The social feed is only shown to Personal accounts and guests.
 */
@Composable
fun BlankHomeScreen(
    userInitial: String,
    onProfileClick: () -> Unit = {},
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomNavBar() },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            TopBar(onProfileClick = onProfileClick, userInitial = userInitial)
            Box(Modifier.fillMaxWidth().weight(1f))
        }
    }
}
