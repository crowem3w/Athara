package social.ahtara.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import social.ahtara.app.R

@Composable
fun MessageTab(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    BottomNavTab(label = "Message", selected = selected, modifier = modifier, onClick = onClick) { tint ->
        Icon(painter = painterResource(R.drawable.ic_message), contentDescription = "Message", tint = tint)
    }
}
