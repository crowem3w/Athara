package social.ahtara.socialfeed.feed

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.net.ApiException
import social.ahtara.socialfeed.model.FeedPost

/** Feed shown on Home, loaded from the backend around the user's main place. */
@Stable
class SocialFeedState internal constructor(
    private val api: FeedApi,
    private val scope: CoroutineScope,
) {
    var posts by mutableStateOf<List<FeedPost>>(emptyList())
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    private var nextCursor: String? = null
    private var area: PlaceSelection? = null

    val canLoadMore: Boolean get() = nextCursor != null && !isLoadingMore

    /** Loads the first page for the first of [places]. [force] reloads even if the same place is already shown. */
    suspend fun load(places: List<PlaceSelection>, force: Boolean) {
        val place = places.firstOrNull()
        if (place == null) {
            posts = emptyList(); nextCursor = null; area = null; notice = null
            return
        }
        if (!force && place == area && posts.isNotEmpty()) return
        try {
            val page = api.page(place.latitude, place.longitude, RADIUS_METERS)
            posts = page.posts
            nextCursor = page.nextCursor
            area = place
            notice = null
        } catch (e: ApiException) {
            // Keep whatever is already on screen; just say why it didn't refresh.
            notice = e.message
        }
    }

    suspend fun loadMore() {
        val cursor = nextCursor ?: return
        val place = area ?: return
        if (isLoadingMore) return
        isLoadingMore = true
        try {
            val page = api.page(place.latitude, place.longitude, RADIUS_METERS, cursor = cursor)
            val seen = posts.mapTo(HashSet()) { it.id }
            posts = posts + page.posts.filter { it.id !in seen }
            nextCursor = page.nextCursor
        } catch (e: ApiException) {
            notice = e.message
        } finally {
            isLoadingMore = false
        }
    }

    /** Optimistic like toggle; [requireAccount] must return true (signed in) or the toggle is skipped. */
    fun toggleLike(post: FeedPost, requireAccount: () -> Boolean) {
        if (!requireAccount()) return
        val want = !post.isLiked
        update(post.id) { it.copy(isLiked = want, likeCount = FeedApi.count(countOf(it) + if (want) 1 else -1)) }
        scope.launch {
            try {
                val (active, likes) = api.setLiked(post.id, want)
                update(post.id) { it.copy(isLiked = active, likeCount = likes?.let(FeedApi::count) ?: it.likeCount) }
            } catch (e: ApiException) {
                update(post.id) { it.copy(isLiked = post.isLiked, likeCount = post.likeCount) }
                notice = e.message
            }
        }
    }

    fun toggleSave(post: FeedPost, requireAccount: () -> Boolean) {
        if (!requireAccount()) return
        val want = !post.isSaved
        update(post.id) { it.copy(isSaved = want) }
        scope.launch {
            try {
                val active = api.setSaved(post.id, want).first
                update(post.id) { it.copy(isSaved = active) }
            } catch (e: ApiException) {
                update(post.id) { it.copy(isSaved = post.isSaved) }
                notice = e.message
            }
        }
    }

    private fun countOf(p: FeedPost) = p.likeCount.toIntOrNull() ?: 0

    private fun update(id: String, change: (FeedPost) -> FeedPost) {
        posts = posts.map { if (it.id == id) change(it) else it }
    }

    private companion object {
        /** Guimaras is small; 50 km covers the whole island from any place on it. */
        const val RADIUS_METERS = 50_000
    }
}

@Composable
fun rememberSocialFeedState(): SocialFeedState {
    val context: Context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return remember { SocialFeedState(FeedApi(context), scope) }
}
