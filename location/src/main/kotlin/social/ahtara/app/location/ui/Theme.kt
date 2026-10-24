package social.ahtara.app.location.ui

import androidx.compose.ui.graphics.Color
import social.ahtara.app.location.search.PlaceCategory

val SurfaceWhite = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1B1B1F)
val Accent = Color(0xFF2F6FED)
val Subtle = Color(0xFF8A8A8E)
val IconBackground = Color(0xFFF1F1F3)

fun PlaceCategory.tint(): Color = when (this) {
    PlaceCategory.COUNTRY -> Color(0xFFE1483C)
    PlaceCategory.PROVINCE -> Color(0xFF2F6FED)
    PlaceCategory.CITY -> Color(0xFF1B9E5A)
}
