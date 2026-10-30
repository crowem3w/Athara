package social.ahtara.network

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** Backend error: `{"error":{"code","message",...}}`. [status] 0 = could not reach the server. */
class ApiException(val status: Int, val code: String, message: String, val extra: JSONObject? = null) :
    Exception(message) {
    val isNetwork: Boolean get() = status == 0
}

/**
 * Tiny JSON-over-HTTP client for the Ahtara gateway. No third-party dependencies.
 * Adds the bearer token, refreshes it once on 401 (single-flight, because the backend has no
 * refresh-token grace window), and turns error bodies into [ApiException].
 */
class ApiClient private constructor(context: Context) {
    val tokens = TokenStore(context)
    private val refreshLock = Mutex()

    /** Called when the refresh token is rejected: the session is over, send the user to sign-in. */
    @Volatile
    var onSessionExpired: (() -> Unit)? = null

    /** Returns the parsed JSON object, or null for an empty body (204). */
    suspend fun request(
        method: String,
        path: String,
        query: Map<String, Any?> = emptyMap(),
        body: JSONObject? = null,
        auth: Boolean = true,
    ): JSONObject? = withContext(Dispatchers.IO) {
        execute(auth) { token ->
            send(method, url(path, query), token, body?.toString()?.toByteArray(), "application/json")
        }
    }

    /** `POST path?query` with a multipart `file` part. */
    suspend fun upload(
        path: String,
        query: Map<String, Any?>,
        bytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg",
    ): JSONObject? = withContext(Dispatchers.IO) {
        val boundary = "ahtara-" + UUID.randomUUID()
        val out = ByteArrayOutputStream()
        out.write(
            ("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n" +
                "Content-Type: $mimeType\r\n\r\n").toByteArray()
        )
        out.write(bytes)
        out.write("\r\n--$boundary--\r\n".toByteArray())
        execute(true) { token ->
            send("POST", url(path, query), token, out.toByteArray(), "multipart/form-data; boundary=$boundary")
        }
    }

    // ---------------------------------------------------------------- internals

    private suspend fun execute(auth: Boolean, call: (String?) -> Pair<Int, String>): JSONObject? {
        var token = if (auth) tokens.access else null
        if (auth && token != null && tokens.accessExpiresSoon) token = refreshIfNeeded(token) ?: token
        var (status, text) = call(token)
        if (status == 401 && auth && tokens.hasSession) {
            val fresh = refreshIfNeeded(token)
            if (fresh != null) {
                val retry = call(fresh)
                status = retry.first
                text = retry.second
            }
        }
        if (status in 200..299) return parse(text)
        throw error(status, text)
    }

    /** Refreshes unless another caller already did (the stored access token differs from [used]). */
    private suspend fun refreshIfNeeded(used: String?): String? = refreshLock.withLock {
        val current = tokens.access
        if (current != null && current != used) return current
        val refresh = tokens.refresh ?: return null
        try {
            val (status, text) = withContext(Dispatchers.IO) {
                send(
                    "POST", url("/auth/refresh", emptyMap()), null,
                    JSONObject().put("refresh_token", refresh).toString().toByteArray(), "application/json",
                )
            }
            if (status in 200..299) {
                val r = JSONObject(text)
                tokens.save(r.getString("access_token"), r.getString("refresh_token"), r.optLong("expires_in", 900))
                tokens.access
            } else {
                if (status == 401 || status == 403) {
                    tokens.clear()
                    onSessionExpired?.invoke()
                }
                null
            }
        } catch (_: ApiException) {
            null // offline: keep the tokens, try again later
        } catch (_: JSONException) {
            null
        }
    }

    private fun send(method: String, url: String, token: String?, body: ByteArray?, contentType: String): Pair<Int, String> {
        val conn = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw ApiException(0, "NETWORK", "Cannot reach the server")
        }
        try {
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/json")
            token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            return status to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } catch (e: IOException) {
            throw ApiException(0, "NETWORK", "Cannot reach the server")
        } finally {
            conn.disconnect()
        }
    }

    private fun url(path: String, query: Map<String, Any?>): String {
        val q = query.entries.filter { it.value != null }
            .joinToString("&") { "${it.key}=${URLEncoder.encode(it.value.toString(), "UTF-8")}" }
        return ApiConfig.apiRoot + path + if (q.isEmpty()) "" else "?$q"
    }

    private fun parse(text: String): JSONObject? =
        if (text.isBlank()) null else try { JSONObject(text) } catch (_: JSONException) { null }

    private fun error(status: Int, text: String): ApiException {
        val err = parse(text)?.optJSONObject("error")
        return ApiException(
            status,
            err?.optString("code").takeUnless { it.isNullOrBlank() } ?: "HTTP_$status",
            err?.optString("message").takeUnless { it.isNullOrBlank() } ?: "Request failed ($status)",
            err,
        )
    }

    companion object {
        @Volatile
        private var instance: ApiClient? = null

        fun get(context: Context): ApiClient = instance ?: synchronized(this) {
            instance ?: run {
                ApiConfig.load(context.applicationContext)
                ApiClient(context.applicationContext).also { instance = it }
            }
        }
    }
}
