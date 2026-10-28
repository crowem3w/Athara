package social.ahtara.travel.source.wikivoyage

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import social.ahtara.travel.model.DestinationContent
import social.ahtara.travel.model.DestinationQuery
import social.ahtara.travel.source.DestinationContentSource







class WikivoyagePublicSource(
    private val language: String = "en",
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val cacheTtlMillis: Long = 6 * 60 * 60 * 1000L,
) : DestinationContentSource {

    private data class Cached(val at: Long, val content: DestinationContent?)

    private val cache = ConcurrentHashMap<String, Cached>()
    private val endpoint get() = "https://$language.wikivoyage.org/w/api.php"

    override suspend fun fetch(query: DestinationQuery, forceRefresh: Boolean): DestinationContent? =
        withContext(Dispatchers.IO) {
            val key = "${query.name.lowercase()}|${"%.2f".format(query.latitude)}|${"%.2f".format(query.longitude)}"
            if (!forceRefresh) {
                cache[key]?.let {
                    if (System.currentTimeMillis() - it.at < cacheTtlMillis) return@withContext it.content
                }
            }
            val content = resolveTitle(query)?.let { loadArticle(it) }
                ?: loadParentRegion(query)?.copy(fallbackFor = query.name)
            if (content == null) Log.w(TAG, "No Wikivoyage article or parent region found for '${query.name}'")
            
            if (content != null) cache[key] = Cached(System.currentTimeMillis(), content)
            content
        }

    override suspend fun article(title: String, forceRefresh: Boolean): DestinationContent? =
        withContext(Dispatchers.IO) {
            val key = "title|${title.lowercase()}"
            if (!forceRefresh) {
                cache[key]?.let {
                    if (System.currentTimeMillis() - it.at < cacheTtlMillis) return@withContext it.content
                }
            }
            loadArticle(title)?.also { cache[key] = Cached(System.currentTimeMillis(), it) }
        }

    



    private fun loadParentRegion(query: DestinationQuery): DestinationContent? {
        val address = runCatching {
            getJson(
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=10&addressdetails=1" +
                    "&accept-language=en&lat=${query.latitude}&lon=${query.longitude}",
            ).optJSONObject("address")
        }.getOrNull() ?: return null

        val tried = candidateNames(query.name).map { it.lowercase() }.toMutableSet()
        for (field in listOf("county", "state_district", "region", "state", "province", "country")) {
            val name = address.optString(field).takeIf { it.isNotBlank() } ?: continue
            if (!tried.add(name.lowercase())) continue
            
            val title = searchTitles(name).firstOrNull {
                it.equals(name, ignoreCase = true) || it.startsWith("$name (", ignoreCase = true)
            } ?: continue
            loadArticle(title)?.let { return it }
        }
        return null
    }

    private fun searchTitles(text: String): List<String> = get(
        "action" to "query", "list" to "search", "srsearch" to text,
        "srnamespace" to "0", "srlimit" to "5",
    ).optJSONObject("query")?.optJSONArray("search")?.titles() ?: emptyList()

    
    private fun candidateNames(name: String): List<String> {
        val first = name.substringBefore(',').trim()
        val stripped = first.replace(
            Regex("\\s+(City|Province|Municipality|Prefecture|District|Region)$", RegexOption.IGNORE_CASE), "",
        ).trim()
        return listOf(name.trim(), first, stripped).filter { it.isNotBlank() }.distinct()
    }

    
    private fun resolveTitle(query: DestinationQuery): String? {
        val nearby = get(
            "action" to "query", "list" to "geosearch",
            "gscoord" to "${query.latitude}|${query.longitude}",
            "gsradius" to "10000", "gslimit" to "10", "gsnamespace" to "0",
        ).optJSONObject("query")?.optJSONArray("geosearch")?.titles() ?: emptyList()

        var exact: String? = null
        var anyHit: String? = null
        for (candidate in candidateNames(query.name)) {
            val hits = searchTitles(candidate)
            hits.firstOrNull { it in nearby }?.let { return it }
            if (exact == null) exact = hits.firstOrNull { it.equals(candidate, ignoreCase = true) }
            
            if (anyHit == null) anyHit = hits.firstOrNull { it.contains(candidate, true) || candidate.contains(it, true) }
        }
        return exact ?: nearby.firstOrNull() ?: anyHit
    }

    private fun loadArticle(title: String): DestinationContent? {
        val page = get(
            "action" to "query", "titles" to title, "redirects" to "1",
            "prop" to "extracts|pageimages|info|revisions",
            "explaintext" to "1", "exsectionformat" to "wiki", "exlimit" to "1",
            "piprop" to "thumbnail", "pithumbsize" to "1000",
            "inprop" to "url",
            "rvprop" to "timestamp|content", "rvslots" to "main", "rvlimit" to "1",
        ).optJSONObject("query")?.optJSONArray("pages")?.optJSONObject(0) ?: return null
        if (page.optBoolean("missing")) return null

        val parsed = ExtractParser.parse(page.optString("extract"))
        if (parsed.lead.isBlank() && parsed.sections.isEmpty()) return null

        val resolvedTitle = page.optString("title", title)
        val leadImage = page.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
        
        
        val images = (listOfNotNull(leadImage) + (runCatching { loadImages(resolvedTitle) }.getOrNull() ?: emptyList()))
            .distinct().take(MAX_IMAGES)

        val revision = page.optJSONArray("revisions")?.optJSONObject(0)
        val edited = revision?.optString("timestamp")?.takeIf { it.isNotBlank() }
        val listings = revision?.optJSONObject("slots")?.optJSONObject("main")?.optString("content")
            ?.let { runCatching { WikitextListings.bySection(it) }.getOrNull() }
            .orEmpty()

        return DestinationContent(
            pageId = page.optLong("pageid"),
            title = resolvedTitle,
            summary = parsed.lead,
            imageUrls = images,
            pageUrl = page.optString("fullurl"),
            updatedAtMillis = edited?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
            sections = parsed.sections.map { it.copy(listings = listings[it.title.lowercase()].orEmpty()) },
        )
    }

    
    private fun loadImages(title: String): List<String> {
        val files = get(
            "action" to "query", "titles" to title, "redirects" to "1",
            "prop" to "images", "imlimit" to "40",
        ).optJSONObject("query")?.optJSONArray("pages")?.optJSONObject(0)?.optJSONArray("images")?.titles()
            ?: return emptyList()

        val photos = files.filter { f ->
            val l = f.lowercase()
            (l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png")) &&
                SKIP_WORDS.none { it in l }
        }.sortedBy { if ("pagebanner" in it.lowercase()) 0 else 1 }.take(MAX_IMAGES)
        if (photos.isEmpty()) return emptyList()

        val pages = get(
            "action" to "query", "titles" to photos.joinToString("|"),
            "prop" to "imageinfo", "iiprop" to "url", "iiurlwidth" to "1000",
        ).optJSONObject("query")?.optJSONArray("pages") ?: return emptyList()

        val urls = HashMap<String, String>()
        for (i in 0 until pages.length()) {
            val p = pages.optJSONObject(i) ?: continue
            val url = p.optJSONArray("imageinfo")?.optJSONObject(0)?.optString("thumburl")
            if (!url.isNullOrBlank()) urls[p.optString("title")] = url
        }
        return photos.mapNotNull { urls[it] }
    }

    private fun get(vararg params: Pair<String, String>): JSONObject {
        val query = (params.toList() + ("format" to "json") + ("formatversion" to "2"))
            .joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        return getJson("$endpoint?$query")
    }

    private fun getJson(url: String): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP ${conn.responseCode} for $url")
            }
            return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    private fun JSONArray.titles(): List<String> =
        (0 until length()).mapNotNull { optJSONObject(it)?.optString("title")?.takeIf { t -> t.isNotBlank() } }

    companion object {
        private const val TAG = "WikivoyageSource"
        private const val MAX_IMAGES = 6
        private val SKIP_WORDS = listOf("flag", "icon", "logo", "map", "coat_of_arms", "symbol", "commons-")

        
        const val DEFAULT_USER_AGENT = "AhtaraApp/1.0 (https://ahtara.social; contact@ahtara.social)"
    }
}