package social.ahtara.travel.source

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URL

internal const val USER_AGENT = "AhtaraSocial/1.0 (https://ahtara.social; contact@ahtara.social)"


internal fun httpRequest(url: String, postBody: String? = null, timeoutMs: Int = 30_000): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    try {
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/json")
        if (postBody != null) {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(postBody.toByteArray()) }
        }
        val code = conn.responseCode
        if (code !in 200..299) throw IOException("HTTP $code from ${URL(url).host}")
        return conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}







internal fun httpGetPublic(url: String, accept: String, maxBytes: Int = 1_500_000, timeoutMs: Int = 15_000): String {
    var current = url
    for (hop in 0..3) {
        val target = URL(current)
        if (target.protocol != "http" && target.protocol != "https") throw IOException("Only http(s) links are supported")
        requirePublicHost(target.host)
        val conn = target.openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects = false
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", accept)
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location") ?: throw IOException("Redirect without a location")
                current = URL(target, location).toString()
                continue
            }
            if (code !in 200..299) throw IOException("HTTP $code from ${target.host}")
            return conn.inputStream.use { readLimited(it, maxBytes) }
        } finally {
            conn.disconnect()
        }
    }
    throw IOException("Too many redirects")
}

private fun requirePublicHost(host: String) {
    val addresses = InetAddress.getAllByName(host)
    val blocked = addresses.any {
        it.isAnyLocalAddress || it.isLoopbackAddress || it.isSiteLocalAddress ||
            it.isLinkLocalAddress || it.isMulticastAddress ||
            
            (it is Inet6Address && (it.address[0].toInt() and 0xFE) == 0xFC)
    }
    if (blocked) throw IOException("$host is not a public web address")
}

private fun readLimited(input: InputStream, maxBytes: Int): String {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        val room = maxBytes - total
        if (n >= room) {
            out.write(buffer, 0, room)
            break
        }
        out.write(buffer, 0, n)
        total += n
    }
    return out.toString("UTF-8")
}