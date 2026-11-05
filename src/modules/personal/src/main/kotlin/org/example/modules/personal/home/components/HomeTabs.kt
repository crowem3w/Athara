package org.example.modules.personal.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.example.modules.personal.R
import org.example.shared.designsystem.theme.Teal
import org.example.shared.designsystem.theme.TealLight
import org.example.shared.designsystem.theme.TextPrimary

// Stand-ins for the bottom-bar and top-bar tabs, built from Material icons.
// The tab file was not part of the uploaded zip: replace this file with the real one.

@Composable
private fun IconTab(
    icon: ImageVector,
    labelRes: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val label = stringResource(labelRes)
    BottomNavTab(label = label, selected = selected, modifier = modifier, onClick = onClick) { tint ->
        Icon(imageVector = icon, contentDescription = null, tint = tint)
    }
}

@Composable
internal fun HomeTab(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    IconTab(Icons.Filled.Home, R.string.personal_tab_home, selected, modifier, onClick)

@Composable
internal fun TransportTab(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    IconTab(Icons.Outlined.DirectionsBus, R.string.personal_tab_transport, selected, modifier, onClick)

@Composable
internal fun MessageTab(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    IconTab(Icons.Outlined.ChatBubbleOutline, R.string.personal_tab_messages, selected, modifier, onClick)

@Composable
internal fun PlacesTab(selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit = {}) =
    IconTab(Icons.Outlined.Place, R.string.personal_tab_places, selected, modifier, onClick)

/** Raised centre button. [visibility] (0..1) fades and shrinks it as the header collapses. */
@Composable
internal fun CreateTab(
    onClick: () -> Unit,
    visibility: () -> Float,
    icon: ImageVector,
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
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

@Composable
internal fun BookmarkTab(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Bookmark, stringResource(R.string.personal_tab_saved), tint = TextPrimary)
    }
}

@Composable
internal fun NotificationTab(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Outlined.Notifications, stringResource(R.string.personal_tab_notifications), tint = TextPrimary)
    }
}

@Composable
internal fun AvatarTab(onClick: () -> Unit, initial: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(TealLight)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = MaterialTheme.typography.labelLarge, color = Teal)
    }
}
