package social.ahtara.business.publish

import android.content.Context
import social.ahtara.business.data.BusinessDatabase


data class PublishedListing(
    val id: Long,
    
    val importedId: String?,
    val name: String,
    
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val hours: String?,
    val phone: String?,
    val website: String?,
)


data class PublishedEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long?,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?,
    val website: String?,
)






class PublishedBusinessSource(context: Context) {
    private val dao = BusinessDatabase.get(context).dao()

    suspend fun listings(): List<PublishedListing> = dao.allListings().map {
        PublishedListing(it.id, it.importedId, it.name, it.category, it.latitude, it.longitude,
            it.description, it.hours, it.phone, it.website)
    }

    suspend fun upcomingEvents(now: Long = System.currentTimeMillis()): List<PublishedEvent> =
        dao.upcomingEvents(now).map {
            PublishedEvent(it.id, it.title, it.description, it.startMillis, it.endMillis,
                it.locationName, it.latitude, it.longitude, it.website)
        }
}