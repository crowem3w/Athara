package social.ahtara.authentication.data

import android.content.Context
import androidx.core.content.edit
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.AuthUser











internal class SessionManager(context: Context) {
    private val app = context.applicationContext
    private val pointer = app.getSharedPreferences(POINTER_PREFS, Context.MODE_PRIVATE)
    private val stores: Map<AccountType, SessionStore> = AccountType.entries.associateWith { type ->
        SessionStore(app.getSharedPreferences(sessionPrefsName(type), Context.MODE_PRIVATE))
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

    private companion object {
        const val POINTER_PREFS = "authentication_active"
        const val KEY_ACTIVE_TYPE = "type"

        fun sessionPrefsName(type: AccountType) = "authentication_session_${type.name.lowercase()}"
    }
}