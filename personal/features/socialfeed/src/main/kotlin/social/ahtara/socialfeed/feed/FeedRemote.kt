package social.ahtara.socialfeed.feed

import androidx.compose.ui.graphics.Color
import java.time.Duration
import java.time.Instant
import org.json.JSONObject
import social.ahtara.network.ApiClient
import social.ahtara.socialfeed.model.FeedPost
import social.ahtara.travel.model.PlaceCategory

/** Feed endpoints (`/feed/*`). Guests can read; liking, saving and posting need a signed-in account. */
internal class FeedRemote(private val api: ApiClient) {

    suspend fun page(lat: Double?, lng: Double?, radiusM: Int = 50_000, category: PlaceCategory? = null, cursor: String? = null): Pair<List<FeedPost>, String?> {
        val json = api.request(
            "GET", "/feed",
            mapOf("lat" to lat, "lng" to lng, "radius_m" to radiusM, "category" to category?.name, "cursor" to cursor),
        ) ?: return emptyList<FeedPost>() to null
        val items = json.optJSONArray("items")
        val posts = (0 until (items?.length() ?: 0)).mapNotNull { items?.optJSONObject(it)?.let(::toPost) }
        return posts to json.optString("next_cursor").ifBlank { null }.takeIf { !json.isNull("next_cursor") }
    }

    /** Returns the new (active, likeCount). */
    suspend fun setLiked(id: String, liked: Boolean): Pair<Boolean, Int?> = toggle(id, "like", liked)
    suspend fun setSaved(id: String, saved: Boolean): Pair<Boolean, Int?> = toggle(id, "save", saved)

    suspend fun create(caption: String, location: String?, lat: Double?, lng: Double?, category: PlaceCategory?, imageMediaId: String? = null, promote: Boolean = false): FeedPost? {
        val body = JSONObject().put("caption", caption).put("promote", promote)
        location?.let { body.put("location_name", it) }
        lat?.let { body.put("latitude", it) }
        lng?.let { body.put("longitude", it) }
        category?.let { body.put("category", it.name) }
        imageMediaId?.let { body.put("image_media_id", it) }
        return api.request("POST", "/feed/posts", body = body)?.let(::toPost)
    }

    private suspend fun toggle(id: String, what: String, on: Boolean): Pair<Boolean, Int?> {
        val r = api.request(if (on) "PUT" else "DELETE", "/feed/posts/$id/$what")
        return (r?.optBoolean("active") ?: on) to (r?.takeIf { it.has("like_count") && !it.isNull("like_count") }?.optInt("like_count"))
    }

    private fun toPost(o: JSONObject): FeedPost {
        val name = o.optString("author_name")
        val authorId = o.optString("author_id")
        return FeedPost(
            id = o.getString("id"),
            authorName = name,
            authorInitial = name.firstOrNull()?.uppercase() ?: "?",
            avatarColor = PALETTE[Math.floorMod(authorId.hashCode(), PALETTE.size)],
            timeAgo = timeAgo(o.optString("created_at")),
            location = o.optString("location").takeUnless { o.isNull("location") }.orEmpty(),
            caption = o.optString("caption"),
            imageColor = PALETTE[Math.floorMod(o.getString("id").hashCode(), PALETTE.size)],
            likeCount = o.optInt("like_count").toString(),
            commentCount = o.optInt("comment_count").toString(),
            isSaved = o.optBoolean("is_saved"),
            isLiked = o.optBoolean("is_liked"),
            imageUrl = o.str("image_url"),
            sourceLabel = o.str("source_label"),
            sourceUrl = o.str("source_url"),
            articleTitle = o.str("article_title"),
            isPromoted = o.optBoolean("is_promoted"),
            category = PlaceCategory.fromName(o.str("category")),
        )
    }

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key).ifBlank { null }

    private fun timeAgo(iso: String): String = runCatching {
        val d = Duration.between(Instant.parse(iso), Instant.now())
        when {
            d.toMinutes() < 1 -> "now"
            d.toMinutes() < 60 -> "${d.toMinutes()}m"
            d.toHours() < 24 -> "${d.toHours()}h"
            else -> "${d.toDays()}d"
        }
    }.getOrDefault("")

    private companion object {
        val PALETTE = listOf(Color(0xFF3F7F6B), Color(0xFF4A78B5), Color(0xFFB5683F), Color(0xFF8A5BA8), Color(0xFFB59A3F), Color(0xFF3F9AB5))
    }
}
