package soc.athara.app.modules.personal.features.socialfeed.model

import androidx.compose.ui.graphics.Color
import soc.athara.app.modules.personal.features.travel.model.PlaceCategory


internal data class FeedPost(
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
    
    val sourceLabel: String? = null,
    val sourceUrl: String? = null,
    val articleTitle: String? = null,
    val isPromoted: Boolean = false,
    val category: PlaceCategory? = null,
)
