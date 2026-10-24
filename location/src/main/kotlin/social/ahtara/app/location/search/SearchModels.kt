package social.ahtara.app.location.search

import org.maplibre.android.geometry.LatLng


enum class PlaceCategory { COUNTRY, CITY, PROVINCE }

data class SearchResult(
    
    val label: String,
    
    val subtitle: String?,
    val latLng: LatLng,
    val category: PlaceCategory,
) {
    
    val fullName: String get() = if (subtitle.isNullOrBlank()) label else "$label, $subtitle"
}