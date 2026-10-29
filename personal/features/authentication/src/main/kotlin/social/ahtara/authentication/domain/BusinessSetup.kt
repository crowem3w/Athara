package social.ahtara.authentication.domain


data class BusinessSetup(
    
    val photoUri: String? = null,
    
    val coverUri: String? = null,
    val about: String = "",
    
    val location: String = "",
    
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    val places: List<PlaceSelection> = emptyList(),
)