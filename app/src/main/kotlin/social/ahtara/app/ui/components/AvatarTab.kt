package social.ahtara.app.ui.components

import social.ahtara.socialfeed.ui.PlaceholderAvatar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import social.ahtara.app.ui.theme.PlaceholderCoral






@Composable
fun AvatarTab(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    color: Color = PlaceholderCoral,
    initial: String = "M",
    size: Dp = 38.dp,
) {
    PlaceholderAvatar(
        color = color,
        initial = initial,
        size = size,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClickLabel = "Profile", onClick = onClick),
    )
}