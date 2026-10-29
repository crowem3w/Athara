package social.ahtara.travel.source.website

import android.util.Xml
import java.io.StringReader
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import social.ahtara.travel.model.PlaceCategory


internal data class RawItem(
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val url: String?,
    
    val types: List<String>,
    
    val keywords: String?,
    val startMillis: Long?,
    val publishedMillis: Long?,
    val latitude: Double?,
    val longitude: Double?,
)





private val TAGS = Regex("<[^>]*>")
private val SPACES = Regex("\\s+")
private val NUMERIC_ENTITY = Regex("&#(x?)([0-9a-fA-F]+);")


internal fun clean(s: String?): String? {
    if (s == null) return null
    var t = TAGS.replace(s, " ")
    t = NUMERIC_ENTITY.replace(t) { m ->
        val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) 10 else 16)
        if (code != null && Character.isValidCodePoint(code)) String(Character.toChars(code)) else " "
    }
    t = t.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
    t = SPACES.replace(t, " ").trim()
    return t.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
}


internal fun absoluteUrl(link: String?, base: String): String? {
    val raw = link?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    val resolved = runCatching { URL(URL(base), raw).toString() }.getOrNull() ?: return null
    return resolved.takeIf { it.startsWith("http://") || it.startsWith("https://") }
}

private fun parseMillis(text: String?): Long? {
    val t = text?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
    return runCatching { OffsetDateTime.parse(t).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { LocalDate.parse(t.take(10)).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull()
}





internal object PageParser {
    private val LD_BLOCK = Regex(
        """<script[^>]*type\s*=\s*["']application/ld\+json["'][^>]*>(.*?)</script>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val META_TAG = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val LINK_TAG = Regex("""<link\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val TITLE_TAG = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    
    fun jsonLd(html: String, base: String): List<RawItem> {
        val out = ArrayList<RawItem>()
        for (block in LD_BLOCK.findAll(html)) {
            val text = block.groupValues[1].trim()
            val node: Any = try {
                if (text.startsWith("[")) JSONArray(text) else JSONObject(text)
            } catch (e: JSONException) {
                continue
            }
            collect(node, base, out, depth = 0)
        }
        return out
    }

    private fun collect(node: Any?, base: String, out: MutableList<RawItem>, depth: Int) {
        if (depth > 4 || out.size >= 100) return
        when (node) {
            is JSONArray -> for (i in 0 until node.length()) collect(node.opt(i), base, out, depth + 1)
            is JSONObject -> {
                node.optJSONArray("@graph")?.let { collect(it, base, out, depth + 1) }
                node.optJSONArray("itemListElement")?.let { collect(it, base, out, depth + 1) }
                node.optJSONObject("item")?.let { collect(it, base, out, depth + 1) }
                toRaw(node, base)?.let(out::add)
            }
        }
    }

    private fun toRaw(node: JSONObject, base: String): RawItem? {
        val types = typesOf(node)
        if (types.none(Classifier::isContentType)) return null
        val title = clean(node.optString("name").ifBlank { node.optString("headline") }) ?: return null
        val geo = node.optJSONObject("geo") ?: node.optJSONObject("location")?.optJSONObject("geo")
        val lat = geo?.optDouble("latitude", Double.NaN)?.takeUnless { it.isNaN() }
        val lon = geo?.optDouble("longitude", Double.NaN)?.takeUnless { it.isNaN() }
        return RawItem(
            title = title,
            description = clean(node.optString("description")),
            imageUrl = imageOf(node.opt("image"), base),
            url = absoluteUrl(node.optString("url").ifBlank { node.optString("@id") }, base),
            types = types,
            keywords = keywordsOf(node.opt("keywords")),
            startMillis = parseMillis(node.optString("startDate")),
            publishedMillis = parseMillis(node.optString("datePublished").ifBlank { node.optString("dateModified") }),
            latitude = lat?.takeIf { it in -90.0..90.0 },
            longitude = lon?.takeIf { it in -180.0..180.0 },
        )
    }

    
    private fun typesOf(node: JSONObject): List<String> {
        val raw = node.opt("@type")
        val names = when (raw) {
            is String -> listOf(raw)
            is JSONArray -> (0 until raw.length()).mapNotNull { raw.optString(it).ifBlank { null } }
            else -> emptyList()
        }
        return names.map { it.substringAfterLast('/').substringAfterLast(':') }
    }

    private fun keywordsOf(v: Any?): String? = when (v) {
        is String -> v
        is JSONArray -> (0 until v.length()).joinToString(" ") { v.optString(it) }
        else -> null
    }?.let(::clean)

    private fun imageOf(v: Any?, base: String): String? = when (v) {
        is String -> absoluteUrl(v, base)
        is JSONObject -> absoluteUrl(v.optString("url").ifBlank { v.optString("contentUrl") }, base)
        is JSONArray -> (0 until v.length()).firstNotNullOfOrNull { imageOf(v.opt(it), base) }
        else -> null
    }

    
    fun openGraph(html: String, pageUrl: String): RawItem? {
        val metas = META_TAG.findAll(html).map { it.value }.toList()
        fun meta(key: String): String? = metas.firstNotNullOfOrNull { tag ->
            val k = attr(tag, "property") ?: attr(tag, "name")
            if (k.equals(key, ignoreCase = true)) attr(tag, "content") else null
        }
        val title = clean(meta("og:title")) ?: clean(TITLE_TAG.find(html)?.groupValues?.get(1)) ?: return null
        return RawItem(
            title = title,
            description = clean(meta("og:description") ?: meta("description")),
            imageUrl = absoluteUrl(meta("og:image"), pageUrl),
            url = absoluteUrl(meta("og:url"), pageUrl) ?: pageUrl,
            types = emptyList(),
            keywords = clean(meta("keywords")),
            startMillis = null,
            publishedMillis = parseMillis(meta("article:published_time")),
            latitude = null,
            longitude = null,
        )
    }

    
    fun feedLink(html: String, pageUrl: String): String? =
        LINK_TAG.findAll(html).map { it.value }.firstOrNull { tag ->
            attr(tag, "rel")?.contains("alternate", ignoreCase = true) == true &&
                attr(tag, "type")?.let { it.contains("rss", true) || it.contains("atom", true) } == true
        }?.let { absoluteUrl(attr(it, "href"), pageUrl) }

    private fun attr(tag: String, name: String): String? =
        Regex("""\b$name\s*=\s*("([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE).find(tag)
            ?.let { it.groupValues[2].ifEmpty { it.groupValues[3] } }
            ?.takeIf { it.isNotEmpty() }
}





internal object FeedParser {
    private val IMG_SRC = Regex("""<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    fun looksLikeFeed(body: String): Boolean {
        val head = body.trimStart().take(1_000).lowercase()
        return head.startsWith("<rss") || head.startsWith("<feed") ||
            (head.startsWith("<?xml") && ("<rss" in head || "<feed" in head || "<rdf:rdf" in head))
    }

    fun parse(xml: String, base: String, maxItems: Int = 60): List<RawItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        val out = ArrayList<RawItem>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && out.size < maxItems) {
                if (event == XmlPullParser.START_TAG && (parser.name == "item" || parser.name == "entry")) {
                    readEntry(parser, base)?.let(out::add)
                }
                event = parser.next()
            }
        } catch (e: XmlPullParserException) {
            
        }
        return out
    }

    private fun readEntry(p: XmlPullParser, base: String): RawItem? {
        val entryTag = p.name
        var title: String? = null
        var link: String? = null
        var summary: String? = null
        var content: String? = null
        var image: String? = null
        var published: Long? = null
        var lat: Double? = null
        var lon: Double? = null
        val tags = ArrayList<String>()

        while (true) {
            val event = p.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            if (event == XmlPullParser.END_TAG && p.name == entryTag) break
            if (event != XmlPullParser.START_TAG) continue
            when (p.name) {
                "title" -> title = p.readInner()
                "link" -> {
                    val href = p.getAttributeValue(null, "href")
                    val rel = p.getAttributeValue(null, "rel")
                    val text = p.readInner()
                    if (href != null) { if (rel == null || rel == "alternate") link = href } else if (text.isNotBlank()) link = text
                }
                "description", "summary" -> summary = p.readInner()
                "content:encoded", "content" -> content = p.readInner()
                "pubDate", "published", "updated", "dc:date" -> if (published == null) published = parseMillis(p.readInner())
                "category" -> {
                    val term = p.getAttributeValue(null, "term")
                    val text = p.readInner()
                    (term ?: text).takeIf { it.isNotBlank() }?.let(tags::add)
                }
                "enclosure", "media:content", "media:thumbnail" -> {
                    val url = p.getAttributeValue(null, "url")
                    val type = p.getAttributeValue(null, "type")
                    p.readInner()
                    if (image == null && url != null && (type == null || type.startsWith("image"))) image = url
                }
                "georss:point" -> {
                    val parts = p.readInner().trim().split(Regex("\\s+"))
                    lat = parts.getOrNull(0)?.toDoubleOrNull()
                    lon = parts.getOrNull(1)?.toDoubleOrNull()
                }
                "geo:lat" -> lat = p.readInner().trim().toDoubleOrNull()
                "geo:long", "geo:lon" -> lon = p.readInner().trim().toDoubleOrNull()
            }
        }

        val heading = clean(title) ?: return null
        val html = content ?: summary
        return RawItem(
            title = heading,
            description = clean(summary) ?: clean(content),
            imageUrl = absoluteUrl(image ?: html?.let { IMG_SRC.find(it)?.groupValues?.get(1) }, base),
            url = absoluteUrl(link, base),
            types = emptyList(),
            keywords = tags.joinToString(" ").ifBlank { null },
            startMillis = null,
            publishedMillis = published,
            latitude = lat?.takeIf { it in -90.0..90.0 },
            longitude = lon?.takeIf { it in -180.0..180.0 },
        )
    }

    
    private fun XmlPullParser.readInner(): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return sb.toString()
            }
        }
        return sb.toString()
    }
}





internal object Classifier {
    private fun types(vararg names: String) = names.toList()

    private val SCHEMA: Map<String, PlaceCategory> = buildMap {
        types("TouristAttraction", "TouristDestination", "LandmarksOrHistoricalBuildings")
            .forEach { put(it, PlaceCategory.ATTRACTIONS) }
        types("Park", "Beach", "BodyOfWater", "LakeBodyOfWater", "RiverBodyOfWater", "SeaBodyOfWater",
            "Mountain", "Volcano", "Waterfall", "Landform", "Reservoir", "Canal", "NatureReserve")
            .forEach { put(it, PlaceCategory.NATURE) }
        types("Restaurant", "CafeOrCoffeeShop", "BarOrPub", "Bakery", "FoodEstablishment", "FastFoodRestaurant",
            "IceCreamShop", "Winery", "Brewery", "Distillery")
            .forEach { put(it, PlaceCategory.FOOD_DRINKS) }
        types("Hotel", "LodgingBusiness", "Resort", "Hostel", "BedAndBreakfast", "Motel", "Campground",
            "VacationRental", "GuestHouse")
            .forEach { put(it, PlaceCategory.ACCOMMODATIONS) }
        types("Museum", "ArtGallery", "PerformingArtsTheater", "PlaceOfWorship", "Church", "CatholicChurch",
            "HinduTemple", "BuddhistTemple", "Mosque", "Synagogue", "HistoricalLandmark")
            .forEach { put(it, PlaceCategory.LOCAL_CULTURE) }
        types("SportsActivityLocation", "AmusementPark", "SkiResort", "GolfCourse", "ExerciseGym",
            "BowlingAlley", "StadiumOrArena", "TouristTrip", "Trip")
            .forEach { put(it, PlaceCategory.ACTIVITIES) }
    }

    
    private val GENERIC = setOf("Place", "LocalBusiness", "Article", "BlogPosting", "NewsArticle", "Event")

    fun isContentType(type: String): Boolean =
        type in SCHEMA || type in GENERIC || isEventType(type)

    private fun isEventType(type: String) = type == "Festival" || type.endsWith("Event")

    fun fromTypes(types: List<String>): PlaceCategory? {
        if (types.any(::isEventType)) return PlaceCategory.EVENTS
        return types.firstNotNullOfOrNull { SCHEMA[it] }
    }

    private val CI = RegexOption.IGNORE_CASE
    private val KEYWORDS: List<Pair<PlaceCategory, Regex>> = listOf(
        PlaceCategory.HIDDEN_GEMS to Regex("""hidden gem|off the beaten|off-the-beaten|secret spot|undiscovered|underrated|lesser[- ]known""", CI),
        PlaceCategory.EVENTS to Regex("""\bfestival\b|\bfiesta\b|\bparade\b|\bconcert\b|\bexhibit(ion)?\b""", CI),
        PlaceCategory.ACCOMMODATIONS to Regex("""\bhotels?\b|\bresorts?\b|\bhostels?\b|\bhomestays?\b|\bguest ?house\b|\blodging\b|where to stay""", CI),
        PlaceCategory.FOOD_DRINKS to Regex("""\brestaurants?\b|\bcafes?\b|\bcafé\b|\bfood\b|\bcuisine\b|\bdelicac(y|ies)\b|\bcoffee\b|\bdish(es)?\b""", CI),
        PlaceCategory.ACTIVITIES to Regex("""\bhik(e|es|ing)\b|\btrek(king)?\b|\bsnorkel|\bdiving\b|\bsurf|\bkayak|\bzipline\b|\bcamping\b|\badventure\b""", CI),
        PlaceCategory.NATURE to Regex("""\bbeach(es)?\b|\bwaterfalls?\b|\bmountains?\b|\bislands?\b|\bforests?\b|\bcaves?\b|\blakes?\b|\brivers?\b|national park|\bwildlife\b|\bnature\b""", CI),
        PlaceCategory.LOCAL_CULTURE to Regex("""\bheritage\b|\bcultur(e|al)\b|\btradition(s|al)?\b|\bmuseum\b|\bchurch\b|\bancestral\b|\bhistor(y|ic|ical)\b|\bcrafts?\b|\bweaving\b|\bfolk\b""", CI),
        PlaceCategory.ATTRACTIONS to Regex("""\battractions?\b|\blandmarks?\b|\bsightseeing\b|must[- ]visit|\bviewpoint\b""", CI),
    )

    fun fromText(vararg parts: String?): PlaceCategory? {
        val text = parts.filterNotNull().joinToString(" ")
        return KEYWORDS.firstOrNull { it.second.containsMatchIn(text) }?.first
    }

    
    fun resolve(item: RawItem, forced: PlaceCategory?): PlaceCategory? =
        forced ?: fromTypes(item.types)
            ?: fromText(item.title, item.keywords, item.description?.take(300))
}