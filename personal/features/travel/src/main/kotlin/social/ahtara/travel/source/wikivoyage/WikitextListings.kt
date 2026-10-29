package social.ahtara.travel.source.wikivoyage

import social.ahtara.travel.model.DestinationListing






internal object WikitextListings {
    private val types = setOf("listing", "see", "do", "buy", "eat", "drink", "sleep", "go")
    private val heading = Regex("^(={2,6})\\s*(.*?)\\s*\\1\\s*$")

    
    fun bySection(wikitext: String): Map<String, List<DestinationListing>> {
        val result = LinkedHashMap<String, MutableList<DestinationListing>>()
        var current: String? = null
        val body = StringBuilder()

        fun flush() {
            current?.let { result.getOrPut(it.lowercase()) { mutableListOf() }.addAll(parseListings(body.toString())) }
            body.clear()
        }

        for (line in wikitext.lines()) {
            val match = heading.matchEntire(line.trim())
            if (match != null && match.groupValues[1].length == 2) {
                flush()
                current = clean(match.groupValues[2])
            } else {
                body.appendLine(line) 
            }
        }
        flush()
        return result
    }

    fun parseListings(text: String): List<DestinationListing> {
        val out = mutableListOf<DestinationListing>()
        var from = 0
        while (true) {
            val start = text.indexOf("{{", from)
            if (start < 0) break
            val end = findClose(text, start)
            if (end < 0) break
            val parts = splitTopLevel(text.substring(start + 2, end))
            if (parts.first().trim().lowercase() in types) toListing(parts.drop(1))?.let(out::add)
            from = end + 2
        }
        return out
    }

    private fun toListing(args: List<String>): DestinationListing? {
        val params = HashMap<String, String>()
        for (arg in args) {
            val eq = arg.indexOf('=')
            if (eq > 0) params[arg.substring(0, eq).trim().lowercase()] = arg.substring(eq + 1).trim()
        }
        val name = clean(params["name"].orEmpty())
        if (name.isBlank()) return null
        fun field(key: String) = clean(params[key].orEmpty()).ifBlank { null }
        return DestinationListing(
            name = name,
            description = field("content"),
            address = field("address"),
            hours = field("hours"),
            price = field("price"),
        )
    }

    
    private fun findClose(s: String, start: Int): Int {
        var depth = 0
        var i = start
        while (i < s.length - 1) {
            when {
                s.startsWith("{{", i) -> { depth++; i += 2 }
                s.startsWith("}}", i) -> { depth--; if (depth == 0) return i; i += 2 }
                else -> i++
            }
        }
        return -1
    }

    
    private fun splitTopLevel(s: String): List<String> {
        val parts = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var i = 0
        while (i < s.length) {
            when {
                s.startsWith("{{", i) || s.startsWith("[[", i) -> { depth++; cur.append(s, i, i + 2); i += 2 }
                s.startsWith("}}", i) || s.startsWith("]]", i) -> { depth--; cur.append(s, i, i + 2); i += 2 }
                s[i] == '|' && depth == 0 -> { parts += cur.toString(); cur.clear(); i++ }
                else -> { cur.append(s[i]); i++ }
            }
        }
        parts += cur.toString()
        return parts
    }

    private val comment = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val ref = Regex("<ref[^>]*/>|<ref[^>]*>.*?</ref>", RegexOption.DOT_MATCHES_ALL)
    private val template = Regex("\\{\\{[^{}]*\\}\\}")
    private val internalLink = Regex("\\[\\[(?:[^\\]|]*\\|)?([^\\]]*)\\]\\]")
    private val labelledUrl = Regex("\\[https?://\\S+\\s+([^\\]]+)\\]")
    private val bareUrl = Regex("\\[https?://[^\\]\\s]+\\]")
    private val tag = Regex("<[^>]+>")
    private val spaces = Regex("\\s+")

    
    fun clean(raw: String): String {
        var t = comment.replace(raw, "")
        t = ref.replace(t, "")
        repeat(3) { t = template.replace(t, "") }
        t = internalLink.replace(t) { it.groupValues[1] }
        t = labelledUrl.replace(t) { it.groupValues[1] }
        t = bareUrl.replace(t, "")
        t = t.replace("'''", "").replace("''", "")
        t = tag.replace(t, "")
        t = t.replace("&nbsp;", " ").replace("&amp;", "&")
        return spaces.replace(t, " ").trim()
    }
}