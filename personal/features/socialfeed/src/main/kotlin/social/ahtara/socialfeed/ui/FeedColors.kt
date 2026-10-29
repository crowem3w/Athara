package social.ahtara.socialfeed.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colors the feed needs from the host app. The feed module has no theme of its own. */
@Immutable
data class FeedColors(
    val divider: Color,
    val heart: Color,
    val textPrimary: Color,
    val textSecondary: Color,
)
