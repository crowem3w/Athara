package social.ahtara.socialfeed.model

import androidx.compose.ui.graphics.Color
import social.ahtara.travel.model.PlaceCategory








data class FeedPost(
    val id: String,
    val authorName: String,
    val authorInitial: String,
    val avatarColor: Color,
    val timeAgo: String,
    val location: String,
    val caption: String,
    val imageColor: Color,
    val likeCount: String,
    val commentCount: String,
    val isSaved: Boolean = false,
    val isLiked: Boolean = false,
    
    val imageUrl: String? = null,
    
    val sourceLabel: String? = null,
    
    val sourceUrl: String? = null,
    
    val articleTitle: String? = null,
    
    val isPromoted: Boolean = false,
    
    val category: PlaceCategory? = null,
)