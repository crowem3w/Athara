package social.ahtara.app.reels

import androidx.compose.ui.graphics.Color

sealed interface ReelSource {
    
    data class Video(val url: String) : ReelSource
}


data class Reel(
    val id: String,
    val source: ReelSource,
    val creatorHandle: String = "",
    val creatorInitial: String = "",
    val avatarColor: Color = Color.Gray,
    val caption: String = "",
    val audioLabel: String = "",
    val likes: Int = 0,
    val comments: Int = 0,
)


fun loadReels(): List<Reel> = emptyList()