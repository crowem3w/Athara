package soc.athara.app.modules.personal.features.socialfeed.data

import soc.athara.app.modules.personal.features.socialfeed.model.FeedPost
import soc.athara.app.modules.personal.features.travel.model.PlaceSelection

internal class FeedPage(val posts: List<FeedPost>, val nextCursor: String?)

/** Failure a repository reports to the UI; [message] is shown as the feed notice. */
internal class FeedException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Source of feed data. The sample implementation is used for now; a backend-backed one
 * (via :network) can replace it without touching the UI or [soc.athara.app.modules.personal.features.socialfeed.feed.SocialFeedState].
 */
internal interface FeedRepository {
    suspend fun page(place: PlaceSelection, cursor: String? = null): FeedPage

    /** Returns the new (active, likeCount). */
    suspend fun setLiked(postId: String, liked: Boolean): Pair<Boolean, Int?>

    suspend fun setSaved(postId: String, saved: Boolean): Boolean
}

internal fun formatCount(n: Int): String = if (n <= 0) "" else n.toString()
