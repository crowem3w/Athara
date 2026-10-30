package social.ahtara.authentication.net

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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Minimal JSON-over-HTTP client for the Ahtara gateway (`/api/v1`).
 *
 *  - Adds `Authorization: Bearer` when a session exists.
 *  - On 401 it rotates the refresh token once (single-flight) and retries the call. If the refresh is
 *    rejected the tokens are wiped and [onSessionExpired] fires so the UI can drop to signed-out.
 *  - Errors surface as [ApiException]; callers never see raw HTTP codes.
 */
class ApiClient private constructor(context: Context) {
    val tokens = TokenStore(context)

    /** Set by the auth repository; called (on any thread) when the refresh token is no longer valid. */
    @Volatile
    var onSessionExpired: () -> Unit = {}

    private val refreshLock = Mutex()

    class Response(val status: Int, val body: String) {
        fun obj(): JSONObject = try { JSONObject(body) } catch (e: JSONException) { throw badBody(e) }
        fun array(): JSONArray = try { JSONArray(body) } catch (e: JSONException) { throw badBody(e) }
        private fun badBody(e: JSONException) = ApiException(status, "BAD_RESPONSE", "Unexpected server response", cause = e)
    }

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap(), auth: Boolean = true) =
        send("GET", path, query, null, auth)

    suspend fun post(path: String, body: JSONObject? = null, auth: Boolean = true) =
        send("POST", path, emptyMap(), body ?: JSONObject(), auth)

    suspend fun patch(path: String, body: JSONObject) = send("PATCH", path, emptyMap(), body, true)

    suspend fun put(path: String, body: Any? = null) = send("PUT", path, emptyMap(), body, true)

    suspend fun delete(path: String) = send("DELETE", path, emptyMap(), null, true)

    /** `POST /media/images?kind=` with a JPEG/PNG/WebP payload; returns `{id, url}`. */
    suspend fun uploadImage(bytes: ByteArray, kind: String): JSONObject {
        val boundary = "ahtara-${UUID.randomUUID()}"
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n".toByteArray())
            write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        return execute(
            "POST", "/media/images", mapOf("kind" to kind), body, "multipart/form-data; boundary=$boundary", true,
        ).obj()
    }

    private suspend fun send(method: String, path: String, query: Map<String, Any?>, json: Any?, auth: Boolean): Response =
        // Bodyless POST/PUT still send "Content-Length: 0" (some proxies answer 411 otherwise).
        execute(
            method, path, query,
            json?.toString()?.toByteArray() ?: if (method == "POST" || method == "PUT") ByteArray(0) else null,
            "application/json", auth,
        )

    private suspend fun execute(
        method: String,
        path: String,
        query: Map<String, Any?>,
        payload: ByteArray?,
        contentType: String,
        auth: Boolean,
    ): Response {
        val sentWith = tokens.accessToken
        val first = call(method, path, query, payload, contentType, if (auth) sentWith else null)
        if (first.status != 401 || !auth || !tokens.hasSession) return check(first)
        if (!refresh(staleAccess = sentWith)) return check(first)
        return check(call(method, path, query, payload, contentType, tokens.accessToken))
    }

    /** Rotates the refresh token. Returns false (and signs out) if the server refuses it. */
    private suspend fun refresh(staleAccess: String?): Boolean = refreshLock.withLock {
        // Another caller may have refreshed while we waited for the lock.
        if (tokens.accessToken != staleAccess && tokens.accessToken != null) return true
        val refresh = tokens.refreshToken ?: return false
        val r = try {
            call("POST", "/auth/refresh", emptyMap(), JSONObject().put("refresh_token", refresh).toString().toByteArray(),
                "application/json", null)
        } catch (_: ApiException) {
            return false // offline: keep the tokens, the next call retries
        }
        if (r.status in 200..299) {
            val o = r.obj()
            tokens.save(o.getString("access_token"), o.getString("refresh_token"))
            true
        } else {
            if (r.status == 401 || r.status == 403) {
                tokens.clear()
                onSessionExpired()
            }
            false
        }
    }

    private fun check(r: Response): Response {
        if (r.status in 200..299) return r
        val err = runCatching { JSONObject(r.body).getJSONObject("error") }.getOrNull()
        throw ApiException(
            status = r.status,
            code = err?.optString("code").takeUnless { it.isNullOrBlank() } ?: "HTTP_${r.status}",
            message = err?.optString("message").takeUnless { it.isNullOrBlank() } ?: "Request failed (${r.status})",
            existingType = err?.optString("existing_type")?.takeUnless { it.isBlank() || it == "null" },
        )
    }

    private suspend fun call(
        method: String,
        path: String,
        query: Map<String, Any?>,
        payload: ByteArray?,
        contentType: String,
        bearer: String?,
    ): Response = withContext(Dispatchers.IO) {
        val qs = query.entries.filter { it.value != null }
            .joinToString("&") { "${it.key}=${URLEncoder.encode(it.value.toString(), "UTF-8")}" }
        val url = URL(ApiConfig.baseUrl + path + if (qs.isEmpty()) "" else "?$qs")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/json")
            bearer?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            if (payload != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(payload.size)
                conn.outputStream.use { it.write(payload) }
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            Response(status, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } catch (e: IOException) {
            throw ApiException(0, "NETWORK", "Can't reach the server. Check your connection.", cause = e)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        @Volatile
        private var instance: ApiClient? = null

        fun get(context: Context): ApiClient =
            instance ?: synchronized(this) { instance ?: ApiClient(context.applicationContext).also { instance = it } }
    }
}
