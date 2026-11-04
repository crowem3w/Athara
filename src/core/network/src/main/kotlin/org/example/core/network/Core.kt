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







data class CoreConfig(
    val baseUrl: String,
    val allowCleartext: Boolean = false,
    val httpLogging: Boolean = false,
    val certificatePins: Map<String, List<String>> = emptyMap(),
)





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

    
    val sync: SyncApi get() = requireGraph().sync

    
    val http: OkHttpClient get() = requireGraph().http

    
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

        
        val refreshApi = retrofit(client()).create(TokenRefreshApi::class.java)
        val http = client {
            addInterceptor(AuthInterceptor(tokens, baseUrl.host))
            authenticator(TokenAuthenticator(tokens, refreshApi))
        }
        val retrofit = retrofit(http)
        return Graph(context, secure, tokens, http, retrofit, retrofit.create(SyncApi::class.java))
    }
}