package social.ahtara.travel.model


data class WebsiteSite(
    
    val id: String,
    val name: String,
    
    val url: String,
    
    val latitude: Double,
    val longitude: Double,
    
    val category: PlaceCategory?,
)


data class WebsiteItem(
    val id: String,
    val siteName: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    
    val url: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
    
    val startMillis: Long? = null,
    val publishedMillis: Long? = null,
)