package social.ahtara.travel.model


data class Festival(
    
    val id: String,
    val name: String,
    val description: String?,
    
    val whenText: String?,
    val latitude: Double?,
    val longitude: Double?,
    val website: String?,
    val imageUrl: String?,
    val sourceUrl: String,
    val attribution: String = "Wikidata · CC0",
)