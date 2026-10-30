package social.ahtara.network

import android.content.Context
import androidx.core.content.edit

/** Access + refresh tokens. Plain private prefs: fine for testing; move to Keystore-backed storage before release. */
class TokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ahtara_tokens", Context.MODE_PRIVATE)

    val access: String? get() = prefs.getString(KEY_ACCESS, null)
    val refresh: String? get() = prefs.getString(KEY_REFRESH, null)
    val hasSession: Boolean get() = refresh != null
    val accessExpiresSoon: Boolean
        get() = System.currentTimeMillis() > prefs.getLong(KEY_EXPIRES_AT, 0L) - 30_000

    fun save(access: String, refresh: String, expiresInSeconds: Long) = prefs.edit(commit = true) {
        putString(KEY_ACCESS, access)
        putString(KEY_REFRESH, refresh)
        putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresInSeconds * 1000)
    }

    fun clear() = prefs.edit(commit = true) { clear() }

    private companion object {
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
