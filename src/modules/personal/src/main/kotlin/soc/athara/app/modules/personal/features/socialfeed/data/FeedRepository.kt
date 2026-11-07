package soc.athara.app.modules.personal.features.socialfeed.data

import soc.athara.app.modules.personal.features.socialfeed.model.FeedPost
import soc.athara.app.modules.personal.features.travel.model.PlaceSelection

internal class FeedPage(val posts: List<FeedPost>, val nextCursor: String?)


internal class FeedException(message: String, cause: Throwable? = null) : Exception(message, cause)





internal interface FeedRepository {
    suspend fun page(place: PlaceSelection, cursor: String? = null): FeedPage

    
    suspend fun setLiked(postId: String, liked: Boolean): Pair<Boolean, Int?>

    suspend fun setSaved(postId: String, saved: Boolean): Boolean
}

internal fun formatCount(n: Int): String = if (n <= 0) "" else n.toString()
