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
