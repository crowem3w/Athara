package social.ahtara.socialfeed.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.socialfeed.model.FeedPost








@Stable
class SocialFeedState internal constructor() {
    var posts by mutableStateOf<List<FeedPost>>(emptyList())
        private set

    
    var notice by mutableStateOf<String?>(null)
        private set

    
    @Suppress("UNUSED_PARAMETER")
    suspend fun load(places: List<PlaceSelection>, force: Boolean) {
        posts = emptyList()
        notice = null
    }
}

@Composable
fun rememberSocialFeedState(): SocialFeedState = remember { SocialFeedState() }