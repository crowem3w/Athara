package social.ahtara.travel.source

import social.ahtara.travel.model.NearbyPlace
import social.ahtara.travel.model.NearbyQuery


interface NearbyPlacesSource {
    
    suspend fun nearby(query: NearbyQuery, forceRefresh: Boolean = false): List<NearbyPlace>
}