package social.ahtara.travel.source.overpass

import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import social.ahtara.travel.model.NearbyPlace
import social.ahtara.travel.model.NearbyQuery
import social.ahtara.travel.model.PlaceCategory
import social.ahtara.travel.model.distanceMeters
import social.ahtara.travel.source.NearbyPlacesSource
import social.ahtara.travel.source.httpRequest






class OverpassSource(
    private val endpoint: String = "https://overpass-api.de/api/interpreter",
    private val cacheTtlMillis: Long = 30 * 60 * 1000L,
) : NearbyPlacesSource {

    private data class Cached(val at: Long, val places: List<NearbyPlace>)

    private val cache = ConcurrentHashMap<String, Cached>()

    override suspend fun nearby(query: NearbyQuery, forceRefresh: Boolean): List<NearbyPlace> =
        withContext(Dispatchers.IO) {
            val key = "%.2f|%.2f|%d|%s|%d".format(
                query.latitude, query.longitude, query.radiusMeters, query.categories.sorted(), query.limit,
            )
            if (!forceRefresh) {
                cache[key]?.let { if (System.currentTimeMillis() - it.at < cacheTtlMillis) return@withContext it.places }
            }
            val body = httpRequest(endpoint, postBody = "data=" + URLEncoder.encode(buildQuery(query), "UTF-8"))
            val places = parse(body, query)
            cache[key] = Cached(System.currentTimeMillis(), places)
            places
        }

    
    private val filters: Map<PlaceCategory, List<String>> = mapOf(
        PlaceCategory.NATURE to listOf(
            """["natural"~"^(beach|waterfall|peak|volcano|hot_spring|cave_entrance|bay|cape)$"]""",
            """["leisure"~"^(park|nature_reserve|garden)$"]""",
            """["boundary"="national_park"]""",
        ),
        PlaceCategory.ATTRACTIONS to listOf(
            """["tourism"~"^(attraction|viewpoint|zoo|theme_park|aquarium)$"]""",
        ),
        PlaceCategory.FOOD_DRINKS to listOf(
            """["amenity"~"^(restaurant|cafe|bar|fast_food)$"]""",
        ),
        PlaceCategory.ACTIVITIES to listOf(
            """["leisure"~"^(water_park|golf_course|marina|dive_centre|sports_centre|miniature_golf)$"]""",
        ),
        PlaceCategory.ACCOMMODATIONS to listOf(
            """["tourism"~"^(hotel|guest_house|hostel|resort|apartment|motel|chalet|camp_site)$"]""",
        ),
        PlaceCategory.LOCAL_CULTURE to listOf(
            """["tourism"~"^(museum|gallery)$"]""",
            """["historic"~"^(monument|memorial|ruins|castle|archaeological_site|fort|church)$"]""",
            """["amenity"~"^(theatre|arts_centre)$"]""",
        ),
    )

    private fun buildQuery(q: NearbyQuery): String {
        val around = "(around:${q.radiusMeters},${q.latitude},${q.longitude})"
        val clauses = q.categories.flatMap { filters[it].orEmpty() }
            .joinToString("") { "nwr$it[\"name\"]$around;" }
        return "[out:json][timeout:30];($clauses);out center tags ${q.limit * 6};"
    }

    private fun parse(body: String, q: NearbyQuery): List<NearbyPlace> {
        val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
        val out = ArrayList<NearbyPlace>()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue
            val name = tags.optString("name").ifBlank { continue }
            val type = el.getString("type")
            val lat = if (el.has("lat")) el.getDouble("lat") else el.optJSONObject("center")?.optDouble("lat") ?: continue
            val lon = if (el.has("lon")) el.getDouble("lon") else el.optJSONObject("center")?.optDouble("lon") ?: continue
            val id = "$type/${el.getLong("id")}"
            val website = tags.optString("website").ifBlank { tags.optString("contact:website") }
                .takeIf { it.startsWith("http://") || it.startsWith("https://") }
            out += NearbyPlace(
                id = "osm:$id",
                name = name,
                category = categoryOf(tags),
                latitude = lat,
                longitude = lon,
                description = tags.optString("description").ifBlank { cuisine(tags) },
                address = address(tags),
                hours = tags.optString("opening_hours").ifBlank { null },
                phone = tags.optString("phone").ifBlank { tags.optString("contact:phone") }.ifBlank { null },
                website = website,
                attribution = ATTRIBUTION,
                sourceUrl = "https://www.openstreetmap.org/$id",
            )
        }
        
        
        val queues = out.groupBy { it.category }.values.map { group ->
            group.sortedWith(
                compareByDescending<NearbyPlace> { listOf(it.website, it.hours, it.description).count { v -> v != null } }
                    .thenBy { distanceMeters(q.latitude, q.longitude, it.latitude, it.longitude) },
            ).iterator()
        }
        val picked = ArrayList<NearbyPlace>()
        while (picked.size < q.limit && queues.any { it.hasNext() }) {
            for (queue in queues) {
                if (picked.size < q.limit && queue.hasNext()) picked += queue.next()
            }
        }
        return picked
    }

    private fun categoryOf(t: JSONObject): PlaceCategory {
        val tourism = t.optString("tourism")
        val amenity = t.optString("amenity")
        val leisure = t.optString("leisure")
        return when {
            tourism in LODGING -> PlaceCategory.ACCOMMODATIONS
            amenity in FOOD -> PlaceCategory.FOOD_DRINKS
            tourism == "museum" || tourism == "gallery" || t.has("historic") ||
                amenity == "theatre" || amenity == "arts_centre" -> PlaceCategory.LOCAL_CULTURE
            t.has("natural") || t.optString("boundary") == "national_park" ||
                leisure in setOf("park", "nature_reserve", "garden") -> PlaceCategory.NATURE
            leisure.isNotEmpty() -> PlaceCategory.ACTIVITIES
            else -> PlaceCategory.ATTRACTIONS
        }
    }

    private fun cuisine(t: JSONObject): String? =
        t.optString("cuisine").takeIf { it.isNotBlank() }
            ?.split(';')?.joinToString(", ") { it.trim().replace('_', ' ').replaceFirstChar(Char::uppercase) }

    private fun address(t: JSONObject): String? =
        listOf("addr:housenumber", "addr:street", "addr:city")
            .mapNotNull { t.optString(it).ifBlank { null } }
            .joinToString(" ").ifBlank { null }

    companion object {
        private val LODGING = setOf("hotel", "guest_house", "hostel", "resort", "apartment", "motel", "chalet", "camp_site")
        private val FOOD = setOf("restaurant", "cafe", "bar", "fast_food")
        const val ATTRIBUTION = "© OpenStreetMap contributors · ODbL"
    }
}