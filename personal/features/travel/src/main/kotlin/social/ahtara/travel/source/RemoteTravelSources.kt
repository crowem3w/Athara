package social.ahtara.travel.source

import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject
import social.ahtara.network.ApiClient
import social.ahtara.network.ApiException
import social.ahtara.travel.model.DestinationContent
import social.ahtara.travel.model.DestinationListing
import social.ahtara.travel.model.DestinationQuery
import social.ahtara.travel.model.DestinationSection
import social.ahtara.travel.model.Festival
import social.ahtara.travel.model.NearbyPlace
import social.ahtara.travel.model.NearbyQuery
import social.ahtara.travel.model.PlaceCategory

/**
 * Travel data through the backend's cached `/travel/*` endpoints instead of calling Overpass, Wikivoyage
 * and Wikidata directly. Same interfaces as the direct sources, so callers can swap them 1:1.
 * Public endpoints: no sign-in needed.
 */
class RemoteNearbyPlacesSource(private val api: ApiClient) : NearbyPlacesSource {
    override suspend fun nearby(query: NearbyQuery, forceRefresh: Boolean): List<NearbyPlace> {
        val json = api.request(
            "GET", "/travel/nearby",
            mapOf(
                "lat" to query.latitude, "lng" to query.longitude, "radius_m" to query.radiusMeters,
                "categories" to query.categories.joinToString(",") { it.name }, "limit" to query.limit,
            ),
            auth = false,
        ) ?: return emptyList()
        return json.optJSONArray("items").objects().mapNotNull { o ->
            val category = PlaceCategory.fromName(o.optString("category")) ?: return@mapNotNull null
            NearbyPlace(
                id = o.getString("id"), name = o.getString("name"), category = category,
                latitude = o.getDouble("latitude"), longitude = o.getDouble("longitude"),
                description = o.str("description"), address = o.str("address"), hours = o.str("hours"),
                phone = o.str("phone"), website = o.str("website"), imageUrl = o.str("image_url"),
                attribution = o.optString("attribution"), sourceUrl = o.str("source_url"),
                claimed = o.optBoolean("claimed"),
            )
        }
    }
}

class RemoteDestinationContentSource(private val api: ApiClient) : DestinationContentSource {
    override suspend fun fetch(query: DestinationQuery, forceRefresh: Boolean): DestinationContent? =
        get("/travel/destination", mapOf("name" to query.name, "lat" to query.latitude, "lng" to query.longitude))

    override suspend fun article(title: String, forceRefresh: Boolean): DestinationContent? =
        get("/travel/destination/article", mapOf("title" to title))

    private suspend fun get(path: String, query: Map<String, Any?>): DestinationContent? {
        val o = try {
            api.request("GET", path, query, auth = false)
        } catch (e: ApiException) {
            if (e.status == 404) return null else throw e // NO_GUIDE
        } ?: return null
        return DestinationContent(
            pageId = o.optLong("page_id"), title = o.optString("title"), summary = o.optString("summary"),
            imageUrls = o.optJSONArray("image_urls").strings(), pageUrl = o.optString("page_url"),
            updatedAtMillis = o.str("updated_at")?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
            sections = o.optJSONArray("sections").objects().map { s ->
                DestinationSection(
                    title = s.optString("title"), text = s.optString("text"),
                    listings = s.optJSONArray("listings").objects().map { l ->
                        DestinationListing(l.optString("name"), l.str("description"), l.str("address"), l.str("hours"), l.str("price"))
                    },
                )
            },
            attribution = o.optString("attribution").ifBlank { "Wikivoyage \u00B7 CC BY-SA" },
            fallbackFor = o.str("fallback_for"),
        )
    }
}

class RemoteFestivalSource(private val api: ApiClient) : FestivalSource {
    override suspend fun festivals(forceRefresh: Boolean): List<Festival> =
        api.request("GET", "/travel/festivals", auth = false)?.optJSONArray("items").objects().map { o ->
            Festival(
                id = o.getString("id"), name = o.getString("name"), description = o.str("description"),
                whenText = o.str("when_text"),
                latitude = if (o.isNull("latitude")) null else o.optDouble("latitude"),
                longitude = if (o.isNull("longitude")) null else o.optDouble("longitude"),
                website = o.str("website"), imageUrl = o.str("image_url"), sourceUrl = o.optString("source_url"),
                attribution = o.optString("attribution").ifBlank { "Wikidata \u00B7 CC0" },
            )
        }
}

private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key).ifBlank { null }
private fun JSONArray?.objects(): List<JSONObject> = this?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()
private fun JSONArray?.strings(): List<String> = this?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
