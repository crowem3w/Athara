package social.ahtara.authentication.domain

import kotlinx.coroutines.flow.StateFlow






interface AuthRepository {
    
    val currentUser: StateFlow<AuthUser?>

    suspend fun signUp(
        name: String,
        
        identifier: String?,
        password: String,
        accountType: AccountType,
        details: AccountDetails = AccountDetails(),
        username: String? = null,
        email: String? = null,
        
        birthday: String? = null,
    ): AuthResult

    
    suspend fun signIn(identifier: String, password: String): AuthResult
    suspend fun signInWithSocial(provider: SocialProvider): AuthResult

    



    suspend fun continueWithSocial(account: SocialAccount): AuthResult = AuthResult.Failure(AuthError.UNKNOWN)

    
    suspend fun signUpWithSocial(account: SocialAccount, identifier: String, accountType: AccountType): AuthResult =
        AuthResult.Failure(AuthError.UNKNOWN)
    
    suspend fun resetPassword(identifier: String, newPassword: String): AuthResult
    suspend fun signOut()

    
    fun isSetupPending(): Boolean = false

    
    fun completeSetup() {}

    
    suspend fun savePersonalSetup(setup: PersonalSetup): Boolean = false

    /** Same as [savePersonalSetup] for BUSINESS accounts; may diverge (business-only fields) later. */
    suspend fun saveBusinessSetup(setup: BusinessSetup): Boolean = false

    



    suspend fun savePlaces(places: List<PlaceSelection>): Boolean = false

    



    suspend fun discardNewAccount(): Boolean = false
}

data class AuthUser(
    
    val id: String,
    
    val identifier: String,
    val displayName: String,
    
    val provider: String,
    val accountType: AccountType = AccountType.PERSONAL,
    val details: AccountDetails = AccountDetails(),
    val username: String? = null,
    val email: String? = null,
    
    val birthday: String? = null,
    
    val about: String? = null,
    val location: String? = null,
    
    val latitude: Double? = null,
    val longitude: Double? = null,
    
    val places: List<PlaceSelection> = emptyList(),
    
    val photoPath: String? = null,
    
    val coverPath: String? = null,
)

enum class SocialProvider { GOOGLE, APPLE }

enum class AuthError { INVALID_CREDENTIALS, IDENTIFIER_TAKEN, USERNAME_TAKEN, EMAIL_TAKEN, ACCOUNT_NOT_FOUND, UNKNOWN }

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    
    data class Failure(val error: AuthError, val existingType: AccountType? = null) : AuthResult
}