package social.ahtara.socialfeed.feed

import android.content.Context
import androidx.compose.ui.graphics.Color
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.json.JSONObject
import social.ahtara.authentication.net.ApiClient
import social.ahtara.socialfeed.model.FeedPost
import social.ahtara.travel.model.PlaceCategory

/** Result page of `GET /feed`. */
internal class FeedPage(val posts: List<FeedPost>, val nextCursor: String?)

/** Thin wrapper over the gateway's `/feed` endpoints (posts, likes, saves). */
internal class FeedApi(context: Context) {
    private val api = ApiClient.get(context)

    suspend fun page(
        lat: Double?,
        lng: Double?,
        radiusMeters: Int,
        category: PlaceCategory? = null,
        cursor: String? = null,
    ): FeedPage {
        val body = api.get(
            "/feed",
            mapOf(
                "lat" to lat, "lng" to lng, "radius_m" to radiusMeters,
                "category" to category?.name, "cursor" to cursor,
            ),
            // Guests can read; sending a token (when there is one) adds is_liked/is_saved.
            auth = api.tokens.hasSession,
        ).obj()
        val items = body.getJSONArray("items")
        return FeedPage(
            posts = (0 until items.length()).map { toPost(items.getJSONObject(it)) },
            nextCursor = body.optString("next_cursor").takeIf { !body.isNull("next_cursor") && it.isNotBlank() },
        )
    }

    /** Returns the new (active, likeCount). */
    suspend fun setLiked(postId: String, liked: Boolean): Pair<Boolean, Int?> = toggle("/feed/posts/$postId/like", liked)

    suspend fun setSaved(postId: String, saved: Boolean): Pair<Boolean, Int?> = toggle("/feed/posts/$postId/save", saved)

    private suspend fun toggle(path: String, on: Boolean): Pair<Boolean, Int?> {
        val r = (if (on) api.put(path) else api.delete(path)).obj()
        return r.getBoolean("active") to if (r.isNull("like_count")) null else r.getInt("like_count")
    }

    private fun toPost(o: JSONObject): FeedPost {
        val author = o.getString("author_name")
        val authorId = o.getString("author_id")
        return FeedPost(
            id = o.getString("id"),
            authorName = author,
            authorInitial = author.trim().firstOrNull()?.uppercase() ?: "?",
            avatarColor = AVATAR_COLORS[Math.floorMod(authorId.hashCode(), AVATAR_COLORS.size)],
            timeAgo = timeAgo(o.optString("created_at")),
            location = o.optStringOrNull("location").orEmpty(),
            caption = o.getString("caption"),
            imageColor = AVATAR_COLORS[Math.floorMod(o.getString("id").hashCode(), AVATAR_COLORS.size)].copy(alpha = 0.35f),
            likeCount = count(o.optInt("like_count")),
            commentCount = count(o.optInt("comment_count")),
            isSaved = o.optBoolean("is_saved"),
            isLiked = o.optBoolean("is_liked"),
            imageUrl = o.optStringOrNull("image_url"),
            sourceLabel = o.optStringOrNull("source_label"),
            sourceUrl = o.optStringOrNull("source_url"),
            articleTitle = o.optStringOrNull("article_title"),
            isPromoted = o.optBoolean("is_promoted"),
            category = PlaceCategory.fromName(o.optStringOrNull("category")),
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    companion object {
        private val AVATAR_COLORS = listOf(
            Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
            Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFEC4899), Color(0xFF6366F1),
        )

        fun count(n: Int): String = if (n <= 0) "" else n.toString()

        internal fun timeAgo(iso: String, now: Instant = Instant.now()): String {
            val then = runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
                ?: runCatching { LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC) }.getOrNull()
                ?: return ""
            val d = Duration.between(then, now)
            return when {
                d.isNegative || d.toMinutes() < 1 -> "Just now"
                d.toMinutes() < 60 -> "${d.toMinutes()}m"
                d.toHours() < 24 -> "${d.toHours()}h"
                d.toDays() < 7 -> "${d.toDays()}d"
                else -> "${d.toDays() / 7}w"
            }
        }
    }
}
