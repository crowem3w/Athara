package social.ahtara.travel.source.opentripmap

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import social.ahtara.travel.model.NearbyPlace
import social.ahtara.travel.model.NearbyQuery
import social.ahtara.travel.model.PlaceCategory
import social.ahtara.travel.source.NearbyPlacesSource
import social.ahtara.travel.source.httpRequest







class OpenTripMapSource(
    private val apiKey: String,
    private val detailCount: Int = 12,
    private val cacheTtlMillis: Long = 6 * 60 * 60 * 1000L,
) : NearbyPlacesSource {

    private data class Cached(val at: Long, val places: List<NearbyPlace>)

    private val cache = ConcurrentHashMap<String, Cached>()
    private val base = "https://api.opentripmap.com/0.1/en/places"

    override suspend fun nearby(query: NearbyQuery, forceRefresh: Boolean): List<NearbyPlace> =
        withContext(Dispatchers.IO) {
            
            if (PlaceCategory.ATTRACTIONS !in query.categories) return@withContext emptyList()
            val key = "%.2f|%.2f|%d|%d".format(query.latitude, query.longitude, query.radiusMeters, query.limit)
            if (!forceRefresh) {
                cache[key]?.let { if (System.currentTimeMillis() - it.at < cacheTtlMillis) return@withContext it.places }
            }
            val list = JSONArray(
                httpRequest(
                    "$base/radius?radius=${query.radiusMeters}&lon=${query.longitude}&lat=${query.latitude}" +
                        "&kinds=interesting_places&rate=2&format=json&limit=${query.limit}&apikey=$apiKey",
                ),
            )
            val basics = (0 until list.length()).map { list.getJSONObject(it) }
                .filter { it.optString("name").isNotBlank() }
                .sortedByDescending { it.optInt("rate") }
            
            val places = coroutineScope {
                basics.mapIndexed { i, o ->
                    async { if (i < detailCount) runCatching { detail(o) }.getOrNull() ?: basic(o) else basic(o) }
                }.awaitAll()
            }
            cache[key] = Cached(System.currentTimeMillis(), places)
            places
        }

    private fun basic(o: JSONObject): NearbyPlace {
        val p = o.getJSONObject("point")
        val xid = o.getString("xid")
        return NearbyPlace(
            id = "otm:$xid", name = o.getString("name"), category = PlaceCategory.ATTRACTIONS,
            latitude = p.getDouble("lat"), longitude = p.getDouble("lon"),
            attribution = ATTRIBUTION, sourceUrl = "https://opentripmap.com/en/card/$xid",
        )
    }

    private fun detail(o: JSONObject): NearbyPlace {
        val xid = o.getString("xid")
        val d = JSONObject(httpRequest("$base/xid/$xid?apikey=$apiKey"))
        return basic(o).copy(
            description = d.optJSONObject("wikipedia_extracts")?.optString("text")?.ifBlank { null },
            imageUrl = d.optJSONObject("preview")?.optString("source")?.ifBlank { null },
            website = d.optString("url").ifBlank { null },
            address = d.optJSONObject("address")?.let {
                listOf("road", "suburb", "city").mapNotNull { k -> it.optString(k).ifBlank { null } }.joinToString(", ")
            }?.ifBlank { null },
        )
    }

    companion object {
        const val ATTRIBUTION = "OpenTripMap · © OpenStreetMap contributors · ODbL"
    }
}