package social.ahtara.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map

@Composable
fun PlacesTab(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    BottomNavTab(label = "Places", selected = selected, modifier = modifier, onClick = onClick) { tint ->
        Icon(imageVector = Icons.Outlined.Map, contentDescription = "Places", tint = tint)
    }
}
