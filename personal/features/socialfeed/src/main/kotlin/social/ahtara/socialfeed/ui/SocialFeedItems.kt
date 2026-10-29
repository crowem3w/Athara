package social.ahtara.socialfeed.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import social.ahtara.socialfeed.feed.SocialFeedState
import social.ahtara.travel.model.PlaceCategory









fun LazyListScope.socialFeedItems(
    state: SocialFeedState,
    isLoading: Boolean,
    hasPlaces: Boolean,
    colors: FeedColors,
    onRequireAccount: () -> Boolean,
    categories: Set<PlaceCategory> = emptySet(),
) {
    val allPosts = state.posts
    val posts = if (categories.isEmpty()) allPosts else allPosts.filter { it.category in categories }
    state.notice?.let { message ->
        item(key = "feed-notice") {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
    if (allPosts.isEmpty() && isLoading) {
        items(3) { PostCardSkeleton(colors) }
    }
    if (posts.isEmpty() && !isLoading) {
        item(key = "feed-empty") {
            Text(
                text = when {
                    !hasPlaces -> "Add a place to see posts here."
                    allPosts.isNotEmpty() -> "No posts in the selected categories yet. Pick other categories or select All."
                    else -> "Nothing here yet."
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
            )
        }
    }
    items(posts, key = { it.id }) { post ->
        PostCard(post = post, colors = colors, onRequireAccount = onRequireAccount)
    }
}