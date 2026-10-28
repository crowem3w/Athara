package social.ahtara.travel.source

import social.ahtara.travel.model.DestinationContent
import social.ahtara.travel.model.DestinationQuery






interface DestinationContentSource {
    



    suspend fun fetch(query: DestinationQuery, forceRefresh: Boolean = false): DestinationContent?

    
    suspend fun article(title: String, forceRefresh: Boolean = false): DestinationContent?
}