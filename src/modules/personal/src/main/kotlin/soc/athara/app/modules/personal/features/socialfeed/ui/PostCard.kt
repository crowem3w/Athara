package soc.athara.app.modules.personal.features.socialfeed.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import soc.athara.app.modules.personal.R
import soc.athara.app.modules.personal.features.socialfeed.model.FeedPost






@Composable
internal fun PostCard(
    post: FeedPost,
    colors: FeedColors,
    onRequireAccount: () -> Boolean,
    modifier: Modifier = Modifier,
    
    onReadMore: (FeedPost) -> Unit = {},
    onLike: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceholderAvatar(
                color = post.avatarColor,
                initial = post.authorInitial,
                size = 44.dp,
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    text = post.authorName,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                if (post.isPromoted) Text(
                    text = stringResource(R.string.socialfeed_promoted),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
                Row {
                    Text(
                        text = post.timeAgo,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = "  •  ${post.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            IconButton(onClick = {  }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.socialfeed_more_options),
                    tint = colors.textSecondary,
                )
            }
        }

        Text(
            text = post.caption,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        PlaceholderImageBlock(
            color = post.imageColor,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        if (post.sourceLabel != null) SourceLine(post, colors, onReadMore)

        EngagementBar(post = post, colors = colors, requireAccount = onRequireAccount, onLike = onLike, onSave = onSave)

        HorizontalDivider(color = colors.divider, thickness = 1.dp)
    }
}

@Composable
private fun EngagementBar(
    post: FeedPost,
    colors: FeedColors,
    requireAccount: () -> Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.socialfeed_like),
            tint = if (post.isLiked) colors.heart else colors.textSecondary,
            modifier = Modifier.clickable { onLike() },
        )
        if (post.likeCount.isNotBlank()) Text(
            text = post.likeCount,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 6.dp),
        )

        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = stringResource(R.string.socialfeed_comments),
            tint = colors.textSecondary,
            modifier = Modifier.clickable { requireAccount() }.padding(start = 20.dp),
        )
        if (post.commentCount.isNotBlank()) Text(
            text = post.commentCount,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 6.dp),
        )

        Icon(
            painter = painterResource(R.drawable.socialfeed_ic_share),
            contentDescription = stringResource(R.string.socialfeed_share),
            tint = colors.textSecondary,
            modifier = Modifier.clickable { requireAccount() }.padding(start = 20.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Outlined.Bookmark,
            contentDescription = stringResource(R.string.socialfeed_save),
            tint = if (post.isSaved) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.clickable { onSave() },
        )
    }
}


@Composable
private fun SourceLine(post: FeedPost, colors: FeedColors, onReadMore: (FeedPost) -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val url = post.sourceUrl
    val action: (() -> Unit)? = when {
        post.articleTitle != null -> ({ onReadMore(post) })
        url != null -> ({ uriHandler.openUri(url) })
        else -> null
    }
    Text(
        text = post.sourceLabel.orEmpty() +
            if (action != null) "  \u2022  " + stringResource(R.string.socialfeed_read_more) else "",
        style = MaterialTheme.typography.bodySmall,
        color = colors.textSecondary,
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .then(if (action != null) Modifier.clickable { action() } else Modifier),
    )
}