package social.ahtara.authentication.data

import android.content.Context
import androidx.core.content.edit
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.AuthUser

/**
 * Session handling with one rule: at most ONE account is signed in, and its session lives in a
 * preferences file of its own type (`authentication_session_personal`, ...).
 *
 * - A small pointer file records which type is active.
 * - [save] wipes the other types' sessions, so two accounts can never be "signed in" at once.
 * - [clear] wipes the active type's file completely (profile cache and setup-pending marker).
 * - The first run after an upgrade moves the old single `authentication_session` file into the
 *   per-type layout, so existing users stay signed in.
 */
internal class SessionManager(context: Context) {
    private val app = context.applicationContext
    private val pointer = app.getSharedPreferences(POINTER_PREFS, Context.MODE_PRIVATE)
    private val stores: Map<AccountType, SessionStore> = AccountType.entries.associateWith { type ->
        SessionStore(app.getSharedPreferences(sessionPrefsName(type), Context.MODE_PRIVATE))
    }

    init {
        migrateLegacySession()
    }

    fun activeType(): AccountType? =
        pointer.getString(KEY_ACTIVE_TYPE, null)?.let { name -> AccountType.entries.firstOrNull { it.name == name } }

    fun read(): AuthUser? = active()?.read()

    fun save(user: AuthUser) {
        AccountType.entries.filter { it != user.accountType }.forEach { stores.getValue(it).clear() }
        stores.getValue(user.accountType).save(user)
        pointer.edit(commit = true) { putString(KEY_ACTIVE_TYPE, user.accountType.name) }
    }

    fun markSetupPending(userId: String) {
        active()?.markSetupPending(userId)
    }

    fun isSetupPending(userId: String): Boolean = active()?.isSetupPending(userId) == true

    fun completeSetup() {
        active()?.completeSetup()
    }

    fun clear() {
        active()?.clear()
        pointer.edit(commit = true) { clear() }
    }

    private fun active(): SessionStore? = activeType()?.let { stores.getValue(it) }

    private fun migrateLegacySession() {
        val legacyPrefs = app.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (legacyPrefs.all.isEmpty()) return
        val legacy = SessionStore(legacyPrefs)
        val user = legacy.read()
        if (user != null && activeType() == null) {
            val pending = legacy.pendingSetupId()
            save(user)
            if (pending == user.id) markSetupPending(user.id)
        }
        legacy.clear()
    }

    private companion object {
        const val POINTER_PREFS = "authentication_active"
        const val KEY_ACTIVE_TYPE = "type"
        const val LEGACY_PREFS = "authentication_session"

        fun sessionPrefsName(type: AccountType) = "authentication_session_${type.name.lowercase()}"
    }
}
