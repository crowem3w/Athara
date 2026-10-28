package social.ahtara.socialfeed.ui

import androidx.compose.foundation.clickable
import social.ahtara.core.ui.components.LocalRequireAccount
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import social.ahtara.core.ui.components.PlaceholderAvatar
import social.ahtara.core.ui.components.PlaceholderImageBlock
import social.ahtara.socialfeed.R
import social.ahtara.socialfeed.model.FeedPost
import social.ahtara.core.ui.theme.Divider
import social.ahtara.core.ui.theme.Heart
import social.ahtara.core.ui.theme.TextPrimary
import social.ahtara.core.ui.theme.TextSecondary






@Composable
fun PostCard(
    post: FeedPost,
    modifier: Modifier = Modifier,
    
    onReadMore: (FeedPost) -> Unit = {},
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
                    color = TextPrimary,
                )
                if (post.isPromoted) Text(
                    text = "Promoted",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Row {
                    Text(
                        text = post.timeAgo,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        text = "  •  ${post.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            IconButton(onClick = {  }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More options",
                    tint = TextSecondary,
                )
            }
        }

        Text(
            text = post.caption,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (post.imageUrl != null) {
            WikimediaImage(
                url = post.imageUrl,
                contentDescription = post.location,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 11f)
                    .clip(RoundedCornerShape(16.dp)),
            )
        } else {
            PlaceholderImageBlock(
                color = post.imageColor,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (post.sourceLabel != null) SourceLine(post, onReadMore)

        EngagementBar(post = post)

        HorizontalDivider(color = Divider, thickness = 1.dp)
    }
}

@Composable
private fun EngagementBar(post: FeedPost, modifier: Modifier = Modifier) {
    // Like / comment / share need an account: guests get the "Account" dialog instead.
    val requireAccount = LocalRequireAccount.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Like",
            tint = if (post.isLiked) Heart else TextSecondary,
            modifier = Modifier.clickable { requireAccount() },
        )
        if (post.likeCount.isNotBlank()) Text(
            text = post.likeCount,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 6.dp),
        )

        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = "Comments",
            tint = TextSecondary,
            modifier = Modifier.clickable { requireAccount() }.padding(start = 20.dp),
        )
        if (post.commentCount.isNotBlank()) Text(
            text = post.commentCount,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 6.dp),
        )

        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = "Share",
            tint = TextSecondary,
            modifier = Modifier.clickable { requireAccount() }.padding(start = 20.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            imageVector = Icons.Outlined.Bookmark,
            contentDescription = "Save",
            tint = if (post.isSaved) TextPrimary else TextSecondary,
        )
    }
}


@Composable
private fun SourceLine(post: FeedPost, onReadMore: (FeedPost) -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val url = post.sourceUrl
    val action: (() -> Unit)? = when {
        post.articleTitle != null -> ({ onReadMore(post) })
        url != null -> ({ uriHandler.openUri(url) })
        else -> null
    }
    Text(
        text = post.sourceLabel.orEmpty() + if (action != null) "  \u2022  Read more" else "",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .then(if (action != null) Modifier.clickable { action() } else Modifier),
    )
}