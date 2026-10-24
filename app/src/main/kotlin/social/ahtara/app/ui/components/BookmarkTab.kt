package social.ahtara.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import social.ahtara.app.ui.theme.TextPrimary

@Composable
fun BookmarkTab(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Outlined.Bookmark,
            contentDescription = "Bookmarks",
            tint = TextPrimary,
        )
    }
}
