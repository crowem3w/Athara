package social.ahtara.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TransportTab(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    BottomNavTab(label = "Transport", selected = selected, modifier = modifier, onClick = onClick) { tint ->
        Icon(imageVector = Icons.Outlined.DirectionsBus, contentDescription = "Transport", tint = tint)
    }
}
