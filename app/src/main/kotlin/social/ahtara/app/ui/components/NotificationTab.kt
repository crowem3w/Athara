package social.ahtara.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import social.ahtara.core.ui.theme.TextPrimary





@Composable
fun NotificationTab(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    hasUnread: Boolean = true,
) {
    BadgedBox(
        badge = { if (hasUnread) Badge() },
        modifier = modifier,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = TextPrimary,
            )
        }
    }
}