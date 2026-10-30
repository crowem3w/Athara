package social.ahtara.authentication.net

import android.content.Context
import androidx.core.content.edit

/** Access + refresh tokens issued by `/auth/*`. Cleared on sign-out and when a refresh is rejected. */
class TokenStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)

    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)

    val hasSession: Boolean get() = refreshToken != null

    fun save(access: String, refresh: String) = prefs.edit(commit = true) {
        putString(KEY_ACCESS, access)
        putString(KEY_REFRESH, refresh)
    }

    fun clear() = prefs.edit(commit = true) { clear() }

    private companion object {
        const val PREFS = "authentication_tokens"
        const val KEY_ACCESS = "access"
        const val KEY_REFRESH = "refresh"
    }
}
