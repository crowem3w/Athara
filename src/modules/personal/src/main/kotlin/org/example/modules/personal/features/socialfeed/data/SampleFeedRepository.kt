package org.example.modules.personal.features.socialfeed.data

import androidx.compose.ui.graphics.Color
import org.example.modules.personal.features.socialfeed.model.FeedPost
import org.example.modules.personal.features.travel.model.PlaceCategory
import org.example.modules.personal.features.travel.model.PlaceSelection
import kotlinx.coroutines.delay

/** In-memory sample posts so the home feed can be built and reviewed before a feed backend exists. */
internal class SampleFeedRepository : FeedRepository {
    private val liked = HashSet<String>()
    private val saved = HashSet<String>()

    override suspend fun page(place: PlaceSelection, cursor: String?): FeedPage {
        delay(400) // let the skeleton and pull-to-refresh indicator show
        val posts = SAMPLE.map { it.copy(isLiked = it.id in liked, isSaved = it.id in saved) }
        return FeedPage(posts, nextCursor = null)
    }

    override suspend fun setLiked(postId: String, liked: Boolean): Pair<Boolean, Int?> {
        if (liked) this.liked.add(postId) else this.liked.remove(postId)
        return liked to null
    }

    override suspend fun setSaved(postId: String, saved: Boolean): Boolean {
        if (saved) this.saved.add(postId) else this.saved.remove(postId)
        return saved
    }

    private companion object {
        val Teal = Color(0xFF14B8A6)
        val Blue = Color(0xFF3B82F6)
        val Amber = Color(0xFFF59E0B)
        val Violet = Color(0xFF8B5CF6)
        val Pink = Color(0xFFEC4899)
        val Green = Color(0xFF10B981)

        fun post(
            id: String, author: String, avatar: Color, time: String, location: String,
            caption: String, likes: Int, comments: Int, category: PlaceCategory,
            promoted: Boolean = false,
        ) = FeedPost(
            id = id,
            authorName = author,
            authorInitial = author.first().uppercase(),
            avatarColor = avatar,
            timeAgo = time,
            location = location,
            caption = caption,
            imageColor = avatar.copy(alpha = 0.35f),
            likeCount = formatCount(likes),
            commentCount = formatCount(comments),
            isPromoted = promoted,
            category = category,
        )

        val SAMPLE = listOf(
            post("s1", "Maria Santos", Teal, "12m", "Alubihod Beach", "Sunrise here is unreal. Low tide, almost nobody around.", 48, 6, PlaceCategory.NATURE),
            post("s2", "Jon Reyes", Blue, "1h", "Trappist Monastery", "Quiet grounds, great for a slow afternoon. Try the dried mangoes.", 31, 3, PlaceCategory.ATTRACTIONS),
            post("s3", "Ana Cruz", Amber, "3h", "Valencia", "Fresh grilled seafood by the pier, cheaper than I expected.", 72, 14, PlaceCategory.FOOD_DRINKS),
            post("s4", "Leo Ramos", Violet, "5h", "Taklong Island", "Snorkelling the marine reserve. Visibility was perfect.", 95, 21, PlaceCategory.ACTIVITIES),
            post("s5", "Isha Lim", Pink, "1d", "Buenavista", "Found a tiny cove behind the old lighthouse path. Worth the walk.", 60, 8, PlaceCategory.HIDDEN_GEMS),
            post("s6", "Seaside Cottages", Green, "1d", "Sibunag", "Beachfront rooms from a weekday rate. Book early for the long weekend.", 12, 1, PlaceCategory.ACCOMMODATIONS, promoted = true),
            post("s7", "Paolo Diaz", Blue, "2d", "Jordan", "Watched the weavers at work. Stayed an hour longer than planned.", 27, 4, PlaceCategory.LOCAL_CULTURE),
            post("s8", "Guimaras Events", Teal, "3d", "Jordan Wharf", "Mango festival lineup is out. Street parade starts at 9 AM.", 54, 9, PlaceCategory.EVENTS),
        )
    }
}
