package social.ahtara.travel.source

import social.ahtara.travel.model.Festival

interface FestivalSource {
    
    suspend fun festivals(forceRefresh: Boolean = false): List<Festival>
}