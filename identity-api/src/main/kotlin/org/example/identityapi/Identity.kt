package org.example.identityapi

import android.content.Context

/**
 * Who is signed in: the only thing feature modules need from the authentication implementation.
 *
 * The module that implements [AuthRepository] (today :authentication) calls [provide] once at
 * startup. Features then use [repository] and the helpers below without depending on it.
 */
object Identity {
    @Volatile
    private var factory: ((Context) -> AuthRepository)? = null

    /** Registers how to obtain the repository. Call once, from Application.onCreate. */
    fun provide(factory: (Context) -> AuthRepository) {
        this.factory = factory
    }

    fun repository(context: Context): AuthRepository {
        val create = factory ?: error("Identity.provide(...) must be called in Application.onCreate")
        return create(context.applicationContext)
    }

    fun isSignedIn(context: Context): Boolean =
        repository(context).currentUser.value != null

    /** True while the signed-in user just created the account and has not finished or skipped the setup step. */
    fun isSetupPending(context: Context): Boolean = repository(context).isSetupPending()

    /** Marks the account setup step as finished or skipped. */
    fun completeSetup(context: Context) = repository(context).completeSetup()

    /** Saves the personal account setup answers on the signed-in user. False if it could not be saved (e.g. offline). */
    suspend fun savePersonalSetup(context: Context, setup: PersonalSetup): Boolean =
        repository(context).savePersonalSetup(setup)

    /** Back to sign-up from the setup step: deletes the just-created account and signs out. */
    suspend fun discardNewAccount(context: Context): Boolean = repository(context).discardNewAccount()

    /** Signs the user out. MainActivity reacts and re-opens the sign-in screen. */
    suspend fun signOut(context: Context) = repository(context).signOut()
}
