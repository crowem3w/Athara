package soc.athara.app.modules.business.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import soc.athara.app.modules.business.R
import soc.athara.app.shared.designsystem.theme.Teal
import soc.athara.app.shared.designsystem.theme.TealLight
import soc.athara.app.shared.designsystem.theme.TextPrimary


@Composable
internal fun ReaderTab(
    onClick: () -> Unit,
    visibility: () -> Float,
    icon: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                val v = visibility()
                alpha = v
                scaleX = 0.6f + 0.4f * v
                scaleY = 0.6f + 0.4f * v
            }
            .clip(CircleShape)
            .background(Teal)
            .clickable(role = Role.Button) { if (visibility() > 0.5f) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(36.dp))
    }
}

@Composable
internal fun BookmarkTab(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Bookmark, stringResource(R.string.business_tab_saved), tint = TextPrimary)
    }
}

@Composable
internal fun NotificationTab(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Notifications, stringResource(R.string.business_tab_notifications), tint = TextPrimary)
    }
}

@Composable
internal fun AvatarTab(onClick: () -> Unit, initial: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(TealLight)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(text = initial, style = MaterialTheme.typography.labelLarge, color = Teal)
        } else {
            Icon(Icons.Filled.Person, stringResource(R.string.business_tab_login), tint = Teal)
        }
    }
}
