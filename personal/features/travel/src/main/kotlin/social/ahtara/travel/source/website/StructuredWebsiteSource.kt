package social.ahtara.travel.source.website

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import social.ahtara.travel.model.PlaceCategory
import social.ahtara.travel.model.WebsiteItem
import social.ahtara.travel.model.WebsiteSite
import social.ahtara.travel.source.WebsiteContentSource
import social.ahtara.travel.source.httpGetPublic











class StructuredWebsiteSource(
    private val maxItemsPerSite: Int = 12,
    private val cacheTtlMillis: Long = 30 * 60 * 1000L,
) : WebsiteContentSource {

    private data class Cached(val at: Long, val items: List<WebsiteItem>)

    private val cache = ConcurrentHashMap<String, Cached>()

    override suspend fun items(site: WebsiteSite, forceRefresh: Boolean): List<WebsiteItem> =
        withContext(Dispatchers.IO) {
            val key = "${site.url}|${site.category}|${site.latitude}|${site.longitude}"
            if (!forceRefresh) {
                cache[key]?.let { if (System.currentTimeMillis() - it.at < cacheTtlMillis) return@withContext it.items }
            }
            val body = httpGetPublic(site.url, ACCEPT)
            val now = System.currentTimeMillis()
            val items = extract(body, site.url)
                .mapNotNull { toItem(it, site) }
                
                .filter { it.category != PlaceCategory.EVENTS || it.startMillis == null || it.startMillis >= now - DAY }
                .distinctBy { it.id }
                .sortedWith(compareBy<WebsiteItem> { it.category != PlaceCategory.EVENTS }.thenBy { it.startMillis ?: Long.MAX_VALUE })
                .take(maxItemsPerSite)
            cache[key] = Cached(System.currentTimeMillis(), items)
            items
        }

    private fun extract(body: String, url: String): List<RawItem> {
        if (FeedParser.looksLikeFeed(body)) return FeedParser.parse(body, url)
        val structured = PageParser.jsonLd(body, url)
        if (structured.size >= 2) return structured
        PageParser.feedLink(body, url)?.let { feedUrl ->
            val feed = runCatching { FeedParser.parse(httpGetPublic(feedUrl, ACCEPT), feedUrl) }.getOrNull()
            if (!feed.isNullOrEmpty()) return feed
        }
        return structured.ifEmpty { listOfNotNull(PageParser.openGraph(body, url)) }
    }

    private fun toItem(raw: RawItem, site: WebsiteSite): WebsiteItem? {
        val category = Classifier.resolve(raw, site.category) ?: return null
        val link = raw.url ?: site.url
        return WebsiteItem(
            id = "web:${site.id}:${Integer.toHexString(link.hashCode())}:${Integer.toHexString(raw.title.hashCode())}",
            siteName = site.name,
            title = raw.title,
            description = raw.description,
            imageUrl = raw.imageUrl,
            url = link,
            category = category,
            latitude = raw.latitude ?: site.latitude,
            longitude = raw.longitude ?: site.longitude,
            startMillis = raw.startMillis,
            publishedMillis = raw.publishedMillis,
        )
    }

    private companion object {
        const val ACCEPT = "text/html,application/xhtml+xml,application/xml,application/rss+xml,application/atom+xml;q=0.9,*/*;q=0.5"
        const val DAY = 24L * 60 * 60 * 1000
    }
}