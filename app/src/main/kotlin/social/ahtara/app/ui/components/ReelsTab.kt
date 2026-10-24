package social.ahtara.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircleOutline

@Composable
fun ReelsTab(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    BottomNavTab(label = "Reels", selected = selected, modifier = modifier, onClick = onClick) { tint ->
        Icon(imageVector = Icons.Outlined.PlayCircleOutline, contentDescription = "Reels", tint = tint)
    }
}
