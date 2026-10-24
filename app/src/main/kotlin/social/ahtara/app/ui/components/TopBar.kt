package social.ahtara.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp







@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    onLogoClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    userInitial: String = "M",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Wordmark(
            onClick = onLogoClick,
            onClickLabel = "Refresh feed",
        )

        Spacer(modifier = Modifier.weight(1f))

        BookmarkTab()
        NotificationTab()

        Spacer(modifier = Modifier.width(4.dp))

        AvatarTab(onClick = onProfileClick, initial = userInitial)
    }
}