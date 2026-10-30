package social.ahtara.socialfeed.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.network.ApiClient
import social.ahtara.network.ApiException
import social.ahtara.socialfeed.model.FeedPost
import social.ahtara.travel.model.PlaceCategory

/** Feed state backed by the backend. Failures keep the posts already on screen and set [notice]. */
@Stable
class SocialFeedState internal constructor(private val remote: FeedRemote) {
    var posts by mutableStateOf<List<FeedPost>>(emptyList())
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    private var nextCursor: String? = null

    /** Loads the first page around the user's first saved place (all areas if there is none). */
    suspend fun load(places: List<PlaceSelection>, force: Boolean, category: PlaceCategory? = null) {
        if (!force && posts.isNotEmpty()) return
        val area = places.firstOrNull()
        try {
            val (items, cursor) = remote.page(area?.latitude, area?.longitude, category = category)
            posts = items
            nextCursor = cursor
            notice = null
        } catch (e: ApiException) {
            notice = message(e)
        }
    }

    /** Appends the next page, if any. */
    suspend fun loadMore(places: List<PlaceSelection>, category: PlaceCategory? = null) {
        val cursor = nextCursor ?: return
        val area = places.firstOrNull()
        try {
            val (items, next) = remote.page(area?.latitude, area?.longitude, category = category, cursor = cursor)
            posts = posts + items.filter { n -> posts.none { it.id == n.id } }
            nextCursor = next
        } catch (e: ApiException) {
            notice = message(e)
        }
    }

    suspend fun toggleLike(id: String) = optimistic(id, { it.copy(isLiked = !it.isLiked) }) { remote.setLiked(id, it.isLiked).second }
    suspend fun toggleSave(id: String) = optimistic(id, { it.copy(isSaved = !it.isSaved) }) { remote.setSaved(id, it.isSaved).second }

    /** Returns true if the post was created (it is shown at the top of the feed). */
    suspend fun createPost(caption: String, place: PlaceSelection?, category: PlaceCategory?, imageMediaId: String? = null, promote: Boolean = false): Boolean =
        try {
            remote.create(caption, place?.name, place?.latitude, place?.longitude, category, imageMediaId, promote)
                ?.let { posts = listOf(it) + posts }
            notice = null
            true
        } catch (e: ApiException) {
            notice = message(e)
            false
        }

    private suspend fun optimistic(id: String, change: (FeedPost) -> FeedPost, call: suspend (FeedPost) -> Int?) {
        val before = posts.firstOrNull { it.id == id } ?: return
        val after = change(before)
        posts = posts.map { if (it.id == id) after else it }
        try {
            call(after)?.let { count -> posts = posts.map { if (it.id == id) it.copy(likeCount = count.toString()) else it } }
        } catch (e: ApiException) {
            posts = posts.map { if (it.id == id) before else it }
            notice = if (e.status == 401) "Sign in to do that" else message(e)
        }
    }

    private fun message(e: ApiException) = if (e.isNetwork) "Can't reach the server" else e.message.orEmpty()
}

@Composable
fun rememberSocialFeedState(): SocialFeedState {
    val app = LocalContext.current.applicationContext
    return remember { SocialFeedState(FeedRemote(ApiClient.get(app))) }
}
