package org.example.modules.personal.features.socialfeed.model

import androidx.compose.ui.graphics.Color
import org.example.modules.personal.features.travel.model.PlaceCategory

/** One post in the home feed. Presentation-ready: counts and times are already formatted. */
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
    /** Shown under the image for article/news style posts. */
    val sourceLabel: String? = null,
    val sourceUrl: String? = null,
    val articleTitle: String? = null,
    val isPromoted: Boolean = false,
    val category: PlaceCategory? = null,
)
