package org.example.core.identityapi

import kotlinx.coroutines.flow.StateFlow

/**
 * Backend-agnostic authentication contract. The UI and :app only depend on
 * this interface. The default implementation is RemoteAuthRepository (REST + tokens);
 * another one can be registered with [org.example.shared.authentication.Authentication.install].
 */
interface AuthRepository {
    /** The signed-in user, or null when signed out. */
    val currentUser: StateFlow<AuthUser?>

    suspend fun signUp(
        name: String,
        /** E.164 mobile number; null when the account is created without one (Personal only). */
        identifier: String?,
        password: String,
        accountType: AccountType,
        details: AccountDetails = AccountDetails(),
        username: String? = null,
        email: String? = null,
        /** ISO date (yyyy-MM-dd). */
        birthday: String? = null,
    ): AuthResult

    /** [identifier] is a username, an email or an E.164 mobile number. */
    suspend fun signIn(identifier: String, password: String): AuthResult
    suspend fun signInWithSocial(provider: SocialProvider): AuthResult

    /**
     * Signs in the user already linked to [account]. Returns Failure(ACCOUNT_NOT_FOUND) when the
     * social account has not registered yet (the caller then asks for a mobile number).
     */
    suspend fun continueWithSocial(account: SocialAccount): AuthResult = AuthResult.Failure(AuthError.UNKNOWN)

    /** Creates a [accountType] account for [account], identified by the mobile number [identifier], and signs in. */
    suspend fun signUpWithSocial(account: SocialAccount, identifier: String, accountType: AccountType): AuthResult =
        AuthResult.Failure(AuthError.UNKNOWN)
    /** [identifier] is a username, an email or an E.164 mobile number. */
    suspend fun resetPassword(identifier: String, newPassword: String): AuthResult
    suspend fun signOut()

    /** True while the signed-in user, who just created the account, still has to see the account setup step. */
    fun isSetupPending(): Boolean = false

    /** Marks the account setup step as done (finished or skipped). */
    fun completeSetup() {}

    /** Saves what the personal account setup step collected on the signed-in user. Returns false if nobody is signed in. */
    suspend fun savePersonalSetup(setup: PersonalSetup): Boolean = false

    /**
     * Deletes the account that was just created and signs out, while its setup step is still pending
     * (the user went back to sign-up). Returns false, doing nothing, for any other account.
     */
    suspend fun discardNewAccount(): Boolean = false
}

data class AuthUser(
    /** String so it can hold a backend UID later (Firebase UID, UUID, ...). */
    val id: String,
    /** The login shown on profile screens: username, else mobile number (E.164), else email. */
    val identifier: String,
    val displayName: String,
    /** "password" or the social provider name. */
    val provider: String,
    val accountType: AccountType = AccountType.PERSONAL,
    val details: AccountDetails = AccountDetails(),
    val username: String? = null,
    val email: String? = null,
    /** ISO date (yyyy-MM-dd), when the account has one. */
    val birthday: String? = null,
    /** Account setup answers; null until filled in. */
    val about: String? = null,
    val location: String? = null,
    /** Absolute path of the profile picture file in app storage. */
    val photoPath: String? = null,
    /** Absolute path of the cover photo file in app storage. */
    val coverPath: String? = null,
)

enum class SocialProvider { GOOGLE, APPLE }

enum class AuthError {
    INVALID_CREDENTIALS, IDENTIFIER_TAKEN, USERNAME_TAKEN, EMAIL_TAKEN, ACCOUNT_NOT_FOUND,
    /** No connection / timeout: nothing was changed, the user can retry. */
    NETWORK,
    /** The server wants a verified mobile number (OTP) before it accepts this request. */
    VERIFICATION_REQUIRED,
    UNKNOWN,
}

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    /** [existingType] is set for IDENTIFIER_TAKEN: the type the number is already registered under. */
    data class Failure(val error: AuthError, val existingType: AccountType? = null) : AuthResult
}
