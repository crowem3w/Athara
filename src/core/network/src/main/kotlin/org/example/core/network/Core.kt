package org.example.core.network

import android.content.Context
import java.util.concurrent.TimeUnit
import okhttp3.CertificatePinner
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.example.core.securestorage.SecureStore
import org.example.core.securestorage.TokenStore
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * @property baseUrl API root, must end with "/" and be https (see [allowCleartext]).
 * @property allowCleartext permit http:// (local dev only; also needs the debug manifest flag).
 * @property httpLogging log request lines (never bodies or the Authorization header).
 * @property certificatePins host to list of "sha256/..." pins; empty = system trust only.
 */
data class CoreConfig(
    val baseUrl: String,
    val allowCleartext: Boolean = false,
    val httpLogging: Boolean = false,
    val certificatePins: Map<String, List<String>> = emptyMap(),
)

/**
 * Process-wide wiring for networking and secure storage. Call [init] once in Application.onCreate.
 * Feature modules build their own Retrofit services with [create]; the generic sync endpoints are [sync].
 */
object Core {
    private class Graph(
        val context: Context,
        val secure: SecureStore,
        val tokens: TokenStore,
        val http: OkHttpClient,
        val retrofit: Retrofit,
        val sync: SyncApi,
    )

    @Volatile private var graph: Graph? = null

    fun init(context: Context, config: CoreConfig) {
        if (graph != null) return
        synchronized(this) { if (graph == null) graph = build(context.applicationContext, config) }
    }

    val appContext: Context get() = requireGraph().context
    val secure: SecureStore get() = requireGraph().secure
    val tokens: TokenStore get() = requireGraph().tokens

    /** Generic sync endpoints (/v1/sync/{resource}). */
    val sync: SyncApi get() = requireGraph().sync

    /** Authenticated client (adds the bearer token for our API host only). Use it to download photos etc. */
    val http: OkHttpClient get() = requireGraph().http

    /** Creates a Retrofit service on the authenticated client, e.g. `Core.create(MyApi::class.java)`. */
    fun <T> create(service: Class<T>): T = requireGraph().retrofit.create(service)

    private fun requireGraph() = graph ?: error("Core.init(...) must be called in Application.onCreate")

    private fun build(context: Context, config: CoreConfig): Graph {
        require(config.baseUrl.endsWith("/")) { "baseUrl must end with '/'" }
        require(config.baseUrl.startsWith("https://") || config.allowCleartext) { "baseUrl must be https" }
        val baseUrl = config.baseUrl.toHttpUrl()

        val secure = SecureStore(context)
        val tokens = TokenStore(secure)

        fun client(extra: OkHttpClient.Builder.() -> Unit = {}): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .apply {
                if (config.certificatePins.isNotEmpty()) {
                    certificatePinner(
                        CertificatePinner.Builder()
                            .apply { config.certificatePins.forEach { (host, pins) -> pins.forEach { add(host, it) } } }
                            .build()
                    )
                }
                if (config.httpLogging) {
                    addNetworkInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                            redactHeader("Authorization")
                        }
                    )
                }
            }
            .apply(extra)
            .build()

        fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(NetworkJson.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
            .build()

        // No interceptor/authenticator on this one: it is what the authenticator calls.
        val refreshApi = retrofit(client()).create(TokenRefreshApi::class.java)
        val http = client {
            addInterceptor(AuthInterceptor(tokens, baseUrl.host))
            authenticator(TokenAuthenticator(tokens, refreshApi))
        }
        val retrofit = retrofit(http)
        return Graph(context, secure, tokens, http, retrofit, retrofit.create(SyncApi::class.java))
    }
}
