package social.ahtara.app.location.search








object SearchPipeline {

    fun search(rawQuery: String, categories: Set<PlaceCategory>): List<SearchResult> {
        val query = rawQuery.trim()
        if (query.isEmpty() || categories.isEmpty()) return emptyList()

        val results = mutableListOf<SearchResult>()
        for (category in listOf(PlaceCategory.COUNTRY, PlaceCategory.PROVINCE, PlaceCategory.CITY)) {
            if (category in categories) results += NominatimClient.search(query, category)
        }
        return results.distinctBy { it.fullName to it.category }
    }
}