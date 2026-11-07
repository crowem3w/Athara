package soc.athara.app.modules.business.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import soc.athara.app.modules.business.R
import soc.athara.app.modules.business.ui.StatusChip
import soc.athara.app.modules.business.ui.Tone
import soc.athara.app.shared.designsystem.theme.Badge as BadgeRed
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TealLight
import soc.athara.app.shared.designsystem.theme.TextPrimary

/** Business identity, open/closed status and notifications. Shared by every tab. */
@Composable
internal fun TopBar(
    businessName: String,
    isOpen: Boolean,
    hasUnread: Boolean,
    onNameClick: () -> Unit,
    onStatusClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(TealLight).clickable(role = Role.Button, onClick = onNameClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(businessName.trim().firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.labelLarge, color = Teal)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                businessName.ifBlank { stringResource(R.string.business_wordmark_description) },
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusChip(
                if (isOpen) "Open" else "Closed",
                if (isOpen) Tone.Good else Tone.Neutral,
                Modifier.clickable(onClick = onStatusClick),
            )
        }
        IconButton(onClick = onNotificationsClick) {
            BadgedBox(badge = { if (hasUnread) Badge(containerColor = BadgeRed) }) {
                Icon(Icons.Outlined.Notifications, stringResource(R.string.business_tab_notifications), tint = TextPrimary)
            }
        }
    }
}
