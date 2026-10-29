package social.ahtara.websites

import android.content.Context
import social.ahtara.websites.data.WebsiteDatabase


data class PublishedWebsite(
    val id: Long,
    val name: String,
    val url: String,
    
    val category: String?,
    val latitude: Double,
    val longitude: Double,
)





class PublishedWebsiteSource(context: Context) {
    private val dao = WebsiteDatabase.get(context).dao()

    suspend fun all(): List<PublishedWebsite> = dao.all().map {
        PublishedWebsite(it.id, it.name, it.url, it.category, it.latitude, it.longitude)
    }
}