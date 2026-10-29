package social.ahtara.travel.model


data class DestinationQuery(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)


data class DestinationContent(
    val pageId: Long,
    val title: String,
    
    val summary: String,
    
    val imageUrls: List<String>,
    
    val pageUrl: String,
    val updatedAtMillis: Long?,
    
    val sections: List<DestinationSection>,
    
    val attribution: String = "Wikivoyage \u00B7 CC BY-SA",
    
    val fallbackFor: String? = null,
)

data class DestinationSection(
    val title: String,
    val text: String,
    
    val listings: List<DestinationListing> = emptyList(),
)


data class DestinationListing(
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val hours: String? = null,
    val price: String? = null,
)