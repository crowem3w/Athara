package social.ahtara.network

import android.content.Context
import androidx.core.content.edit

/**
 * Where the backend lives. Defaults to the Termux/proot gateway on the same phone.
 * Change at runtime (no rebuild) with [setBaseUrl], e.g. to a LAN IP for another device.
 */
object ApiConfig {
    const val DEFAULT_BASE_URL = "http://127.0.0.1:8080"

    private const val PREFS = "ahtara_api"
    private const val KEY_URL = "base_url"
    private const val KEY_ENABLED = "backend_enabled"

    @Volatile
    var baseUrl: String = DEFAULT_BASE_URL
        private set

    /** `http://host:port/api/v1` */
    val apiRoot: String get() = baseUrl.trimEnd('/') + "/api/v1"

    internal fun load(context: Context) {
        baseUrl = prefs(context).getString(KEY_URL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(context: Context, url: String) {
        val clean = url.trim().trimEnd('/')
        prefs(context).edit { putString(KEY_URL, clean) }
        baseUrl = clean.ifBlank { DEFAULT_BASE_URL }
    }

    /** Kill switch: false = the app runs fully on its local Room data, as before the migration. */
    fun backendEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setBackendEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
