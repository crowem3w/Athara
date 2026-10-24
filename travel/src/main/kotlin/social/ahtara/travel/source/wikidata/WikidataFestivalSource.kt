package social.ahtara.travel.source.wikidata

import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import social.ahtara.travel.model.Festival
import social.ahtara.travel.source.FestivalSource
import social.ahtara.travel.source.httpRequest









class WikidataFestivalSource(
    private val countryQid: String = "Q928",
    private val limit: Int = 200,
    private val cacheTtlMillis: Long = 24 * 60 * 60 * 1000L,
) : FestivalSource {

    @Volatile private var cached: Pair<Long, List<Festival>>? = null

    override suspend fun festivals(forceRefresh: Boolean): List<Festival> = withContext(Dispatchers.IO) {
        cached?.let { (at, list) ->
            if (!forceRefresh && System.currentTimeMillis() - at < cacheTtlMillis) return@withContext list
        }
        val url = "https://query.wikidata.org/sparql?format=json&query=" + URLEncoder.encode(query(), "UTF-8")
        val list = parse(httpRequest(url))
        cached = System.currentTimeMillis() to list
        list
    }

    internal fun query(): String = """
        SELECT ?i ?iLabel ?iDescription (SAMPLE(?c) AS ?coord) (SAMPLE(?dl) AS ?day) (SAMPLE(?w) AS ?web) (SAMPLE(?img) AS ?image)
        WHERE {
          ?i wdt:P31/wdt:P279* wd:Q132241; wdt:P17 wd:$countryQid.
          OPTIONAL { ?i wdt:P625 ?c }
          OPTIONAL { ?i wdt:P837 ?d. ?d rdfs:label ?dl FILTER(LANG(?dl) = "en") }
          OPTIONAL { ?i wdt:P856 ?w }
          OPTIONAL { ?i wdt:P18 ?img }
          SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
        }
        GROUP BY ?i ?iLabel ?iDescription
        LIMIT $limit
    """.trimIndent()

    internal fun parse(body: String): List<Festival> {
        val rows = JSONObject(body).getJSONObject("results").getJSONArray("bindings")
        val out = ArrayList<Festival>()
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            val uri = r.value("i") ?: continue
            val qid = uri.substringAfterLast('/')
            val name = r.value("iLabel")?.takeIf { it != qid } ?: continue 
            val (lat, lon) = r.value("coord")?.let { parsePoint(it) } ?: (null to null)
            out += Festival(
                id = "wd:$qid",
                name = name,
                description = r.value("iDescription"),
                whenText = r.value("day"),
                latitude = lat,
                longitude = lon,
                website = r.value("web")?.takeIf { it.startsWith("http://") || it.startsWith("https://") },
                imageUrl = r.value("image")?.let { commonsThumb(it) },
                sourceUrl = "https://www.wikidata.org/wiki/$qid",
            )
        }
        return out.sortedBy { it.name }
    }

    private fun JSONObject.value(key: String): String? =
        optJSONObject(key)?.optString("value")?.ifBlank { null }

    
    private fun parsePoint(wkt: String): Pair<Double, Double>? {
        val m = Regex("""Point\(\s*(-?[\d.]+)\s+(-?[\d.]+)\s*\)""").find(wkt) ?: return null
        val lon = m.groupValues[1].toDoubleOrNull() ?: return null
        val lat = m.groupValues[2].toDoubleOrNull() ?: return null
        return lat to lon
    }

    
    private fun commonsThumb(url: String): String =
        url.replace("http://", "https://") + if ('?' in url) "" else "?width=800"
}