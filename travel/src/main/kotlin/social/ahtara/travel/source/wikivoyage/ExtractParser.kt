package social.ahtara.travel.source.wikivoyage

import social.ahtara.travel.model.DestinationSection

internal data class ParsedExtract(
    val lead: String,
    val sections: List<DestinationSection>,
)





internal object ExtractParser {
    private val heading = Regex("^(={2,6})\\s*(.*?)\\s*\\1$")

    fun parse(extract: String): ParsedExtract {
        val lead = StringBuilder()
        val sections = mutableListOf<DestinationSection>()
        var title: String? = null
        val body = StringBuilder()

        fun flush() {
            title?.let { t ->
                val text = body.toString().trim()
                if (text.isNotEmpty()) sections += DestinationSection(t, text)
            }
            body.clear()
        }

        for (raw in extract.lines()) {
            val line = raw.trim()
            val match = heading.matchEntire(line)
            when {
                match != null && match.groupValues[1].length == 2 -> {
                    flush()
                    title = match.groupValues[2]
                }
                match != null -> Unit 
                title == null -> lead.appendLine(line)
                else -> body.appendLine(line)
            }
        }
        flush()
        return ParsedExtract(lead.toString().trim(), sections)
    }
}