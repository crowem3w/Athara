package social.ahtara.authentication.domain






object ServiceArea {
    const val NAME = "Guimaras, Philippines"
    const val COUNTRY_CODE = "ph"

    
    const val CENTER_LAT = 10.59
    const val CENTER_LNG = 122.63

    
    const val NORTH = 10.80
    const val SOUTH = 10.40
    const val WEST = 122.45
    const val EAST = 122.85

    
    val default = PlaceSelection(NAME, CENTER_LAT, CENTER_LNG)

    



    val municipalities: List<PlaceSelection> = listOf(
        PlaceSelection("Buenavista, Guimaras", 10.6903, 122.6275),
        PlaceSelection("Jordan, Guimaras", 10.6558, 122.5997),
        PlaceSelection("Nueva Valencia, Guimaras", 10.5333, 122.5833),
        PlaceSelection("San Lorenzo, Guimaras", 10.5981, 122.7172),
        PlaceSelection("Sibunag, Guimaras", 10.5139, 122.5392),
    )

    
    val quickPicks: List<PlaceSelection> = listOf(default) + municipalities

    fun contains(latitude: Double, longitude: Double): Boolean =
        latitude in SOUTH..NORTH && longitude in WEST..EAST

    fun contains(place: PlaceSelection): Boolean = contains(place.latitude, place.longitude)
}