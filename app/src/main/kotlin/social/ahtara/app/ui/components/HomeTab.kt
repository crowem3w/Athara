package social.ahtara.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home

@Composable
fun HomeTab(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    BottomNavTab(label = "Home", selected = selected, modifier = modifier, onClick = onClick) { tint ->
        Icon(imageVector = Icons.Filled.Home, contentDescription = "Home", tint = tint)
    }
}
