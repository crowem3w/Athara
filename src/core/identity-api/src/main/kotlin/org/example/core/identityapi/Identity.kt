package org.example.core.identityapi

import android.content.Context







object Identity {
    @Volatile
    private var factory: ((Context) -> AuthRepository)? = null

    
    fun provide(factory: (Context) -> AuthRepository) {
        this.factory = factory
    }

    fun repository(context: Context): AuthRepository {
        val create = factory ?: error("Identity.provide(...) must be called in Application.onCreate")
        return create(context.applicationContext)
    }

    fun isSignedIn(context: Context): Boolean =
        repository(context).currentUser.value != null

    
    fun isSetupPending(context: Context): Boolean = repository(context).isSetupPending()

    
    fun completeSetup(context: Context) = repository(context).completeSetup()

    
    suspend fun savePersonalSetup(context: Context, setup: PersonalSetup): Boolean =
        repository(context).savePersonalSetup(setup)

    
    suspend fun discardNewAccount(context: Context): Boolean = repository(context).discardNewAccount()

    
    suspend fun signOut(context: Context) = repository(context).signOut()
}