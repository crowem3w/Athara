package org.example.authentication.data

import org.example.identityapi.AccountDetails
import org.example.identityapi.AccountType
import org.example.identityapi.AuthUser
import org.example.securestorage.SecureStore
import org.json.JSONObject

/**
 * Remembers the signed-in profile across restarts, encrypted with the Keystore ([SecureStore]).
 * It is only a cache of what the server returned; tokens live in the core TokenStore.
 */
internal class SessionStore(private val secure: SecureStore) {

    fun read(): AuthUser? {
        val raw = secure.getString(KEY_USER) ?: return null
        return runCatching { fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun save(user: AuthUser) = secure.putString(KEY_USER, toJson(user).toString())

    /** A just-created account still has to go through the account setup step. */
    fun markSetupPending(userId: String) = secure.putString(KEY_SETUP_PENDING, userId)

    fun isSetupPending(userId: String): Boolean = secure.getString(KEY_SETUP_PENDING) == userId

    fun completeSetup() = secure.remove(KEY_SETUP_PENDING)

    fun clear() {
        secure.remove(KEY_USER)
        secure.remove(KEY_SETUP_PENDING)
    }

    private fun toJson(u: AuthUser) = JSONObject().apply {
        put("id", u.id)
        put("identifier", u.identifier)
        put("name", u.displayName)
        put("provider", u.provider)
        put("accountType", u.accountType.name)
        put("industry", u.details.industry)
        put("phone", u.details.phone)
        put("registrationId", u.details.registrationId)
        put("username", u.username)
        put("email", u.email)
        put("birthday", u.birthday)
        put("about", u.about)
        put("location", u.location)
        put("photoPath", u.photoPath)
        put("coverPath", u.coverPath)
    }

    private fun fromJson(o: JSONObject) = AuthUser(
        id = o.getString("id"),
        identifier = o.str("identifier").orEmpty(),
        displayName = o.str("name").orEmpty(),
        provider = o.str("provider") ?: "password",
        accountType = AccountType.fromName(o.str("accountType")),
        details = AccountDetails(o.str("industry"), o.str("phone"), o.str("registrationId")),
        username = o.str("username"),
        email = o.str("email"),
        birthday = o.str("birthday"),
        about = o.str("about"),
        location = o.str("location"),
        photoPath = o.str("photoPath"),
        coverPath = o.str("coverPath"),
    )

    private fun JSONObject.str(key: String): String? = if (isNull(key)) null else getString(key)

    private companion object {
        const val KEY_USER = "session_user"
        const val KEY_SETUP_PENDING = "setup_pending_id"
    }
}
