package social.ahtara.authentication.domain

/**
 * The only area the app covers: Guimaras, Philippines.
 * Single source of truth for the guest default place, map search limits and camera limits.
 * Coordinates are approximate; adjust here if the box needs tightening.
 */
object ServiceArea {
    const val NAME = "Guimaras, Philippines"
    const val COUNTRY_CODE = "ph"

    // Island centre.
    const val CENTER_LAT = 10.59
    const val CENTER_LNG = 122.63

    // Bounding box (slightly padded around the island).
    const val NORTH = 10.80
    const val SOUTH = 10.40
    const val WEST = 122.45
    const val EAST = 122.85

    /** Default place for guests and for anyone with no saved Guimaras place. */
    val default = PlaceSelection(NAME, CENTER_LAT, CENTER_LNG)

    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in SOUTH..NORTH && longitude in WEST..EAST

    fun contains(place: PlaceSelection): Boolean = contains(place.latitude, place.longitude)
}
