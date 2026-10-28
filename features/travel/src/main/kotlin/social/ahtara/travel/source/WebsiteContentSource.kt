package social.ahtara.travel.source

import social.ahtara.travel.model.WebsiteItem
import social.ahtara.travel.model.WebsiteSite


interface WebsiteContentSource {
    
    suspend fun items(site: WebsiteSite, forceRefresh: Boolean = false): List<WebsiteItem>
}