package social.ahtara.travel.model


enum class PlaceCategory(val label: String) {
    NATURE("Nature"),
    ATTRACTIONS("Attractions"),
    FOOD_DRINKS("Food & Drinks"),
    ACTIVITIES("Activities"),
    HIDDEN_GEMS("Hidden Gems"),
    ACCOMMODATIONS("Accommodations"),
    LOCAL_CULTURE("Local Culture"),
    EVENTS("Events");

    companion object {
        




        fun fromName(name: String?): PlaceCategory? = when (name) {
            null -> null
            "ATTRACTION" -> ATTRACTIONS
            "FOOD" -> FOOD_DRINKS
            "STAY" -> ACCOMMODATIONS
            else -> entries.firstOrNull { it.name == name }
        }
    }
}


data class NearbyPlace(
    
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val address: String? = null,
    val hours: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val imageUrl: String? = null,
    
    val attribution: String,
    val sourceUrl: String? = null,
    



    val claimed: Boolean = false,
)

data class NearbyQuery(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 5_000,
    val categories: Set<PlaceCategory> = PlaceCategory.entries.toSet(),
    val limit: Int = 40,
)