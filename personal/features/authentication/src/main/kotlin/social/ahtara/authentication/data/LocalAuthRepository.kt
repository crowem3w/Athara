package social.ahtara.authentication.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import social.ahtara.authentication.domain.AccountDetails
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.AuthError
import social.ahtara.authentication.domain.AuthRepository
import social.ahtara.authentication.domain.AuthResult
import social.ahtara.authentication.domain.AuthUser
import social.ahtara.authentication.domain.BusinessSetup
import social.ahtara.authentication.domain.PersonalSetup
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.SocialAccount
import social.ahtara.authentication.domain.SocialProvider
import social.ahtara.authentication.domain.toJsonString









internal class LocalAuthRepository(
    private val stores: AccountStores,
    private val session: SessionManager,
    private val photos: ProfilePhotoStore,
    private val flow: DeviceFlowStore,
    private val hasher: PasswordHasher = PasswordHasher(),
) : AuthRepository {

    private val _currentUser = MutableStateFlow(session.read())
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    

    override suspend fun signUp(
        name: String,
        identifier: String?,
        password: String,
        accountType: AccountType,
        details: AccountDetails,
        username: String?,
        email: String?,
        birthday: String?,
    ): AuthResult {
        val key = identifier?.let(::normalize)?.ifBlank { null }
        val handle = username?.let(::normalize)?.ifBlank { null }
        val mail = email?.let(::normalize)?.ifBlank { null }
        findConflict(key, handle, mail)?.let { return it }
        val hash = withContext(Dispatchers.Default) { hasher.hash(password) }
        val entity = UserEntity(
            identifier = key,
            displayName = name.trim(),
            passwordHash = hash,
            provider = PROVIDER_PASSWORD,
            createdAt = System.currentTimeMillis(),
            accountType = accountType.name,
            industry = details.industry?.trim()?.ifBlank { null },
            phone = details.phone?.trim()?.ifBlank { null },
            registrationId = details.registrationId?.trim()?.ifBlank { null },
            username = handle,
            email = mail,
            birthday = birthday?.trim()?.ifBlank { null },
        )
        return register(entity) { findConflict(key, handle, mail) ?: AuthResult.Failure(AuthError.IDENTIFIER_TAKEN) }
    }

    override suspend fun signIn(identifier: String, password: String): AuthResult {
        val user = stores.directory().byAnyId(normalize(identifier))
        val stored = user?.passwordHash
        if (user == null || stored == null) return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        val ok = withContext(Dispatchers.Default) { hasher.verify(password, stored) }
        return if (ok) startSession(user) else AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
    }

    
    override suspend fun signInWithSocial(provider: SocialProvider): AuthResult {
        val key = "demo.${provider.name.lowercase()}"
        stores.directory().byIdentifier(key)?.let { return startSession(it) }
        val entity = UserEntity(
            identifier = key,
            displayName = "Demo ${provider.name.lowercase().replaceFirstChar { it.uppercase() }} User",
            passwordHash = null,
            provider = provider.name.lowercase(),
            createdAt = System.currentTimeMillis(),
        )
        return register(entity) {
            stores.directory().byIdentifier(key)?.let { startSession(it) } ?: AuthResult.Failure(AuthError.UNKNOWN)
        }
    }

    override suspend fun continueWithSocial(account: SocialAccount): AuthResult {
        val user = stores.directory().byProvider(account.provider.name.lowercase(), account.subject)
            ?: return AuthResult.Failure(AuthError.ACCOUNT_NOT_FOUND)
        return startSession(user)
    }

    override suspend fun signUpWithSocial(
        account: SocialAccount,
        identifier: String,
        accountType: AccountType,
    ): AuthResult {
        val key = normalize(identifier)
        stores.directory().byIdentifier(key)?.let { return identifierTaken(it) }
        val entity = UserEntity(
            identifier = key,
            displayName = account.displayName.trim().ifBlank { account.email.orEmpty() },
            passwordHash = null,
            provider = account.provider.name.lowercase(),
            createdAt = System.currentTimeMillis(),
            accountType = accountType.name,
            providerId = account.subject,
        )
        return register(entity) {
            stores.directory().byIdentifier(key)?.let { identifierTaken(it) }
                ?: AuthResult.Failure(AuthError.IDENTIFIER_TAKEN)
        }
    }

    override suspend fun resetPassword(identifier: String, newPassword: String): AuthResult {
        val user = stores.directory().byAnyId(normalize(identifier))
            ?: return AuthResult.Failure(AuthError.ACCOUNT_NOT_FOUND)
        val hash = withContext(Dispatchers.Default) { hasher.hash(newPassword) }
        daoFor(user).updatePasswordHash(user.id, hash)
        return AuthResult.Success(user.toAuthUser())
    }

    

    

    override suspend fun signOut() = endSession()

    override fun isSetupPending(): Boolean =
        _currentUser.value?.let { session.isSetupPending(it.id) } == true

    override fun completeSetup() = session.completeSetup()

    
    override suspend fun discardNewAccount(): Boolean {
        val user = _currentUser.value ?: return false
        val id = user.id.toLongOrNull() ?: return false
        if (!session.isSetupPending(user.id)) return false
        withContext(Dispatchers.IO) { photos.delete(user.id) }
        stores.of(user.accountType).deleteById(id)
        endSession()
        return true
    }

    

    

    override suspend fun savePersonalSetup(setup: PersonalSetup): Boolean = saveProfileSetup(
        aboutInput = setup.about, locationInput = setup.location,
        latitudeInput = setup.latitude, longitudeInput = setup.longitude,
        places = setup.places, photoUri = setup.photoUri, coverUri = setup.coverUri,
    )

    override suspend fun saveBusinessSetup(setup: BusinessSetup): Boolean = saveProfileSetup(
        aboutInput = setup.about, locationInput = setup.location,
        latitudeInput = setup.latitude, longitudeInput = setup.longitude,
        places = setup.places, photoUri = setup.photoUri, coverUri = setup.coverUri,
    )

    
    private suspend fun saveProfileSetup(
        aboutInput: String,
        locationInput: String,
        latitudeInput: Double?,
        longitudeInput: Double?,
        places: List<PlaceSelection>,
        photoUri: String?,
        coverUri: String?,
    ): Boolean {
        val user = _currentUser.value ?: return false
        val id = user.id.toLongOrNull() ?: return false
        val about = aboutInput.trim().ifBlank { null }

        val main = places.firstOrNull()
        val location = main?.name ?: locationInput.trim().ifBlank { null }
        val latitude = if (location != null) main?.latitude ?: latitudeInput else null
        val longitude = if (location != null) main?.longitude ?: longitudeInput else null
        val placesJson = places.takeIf { it.isNotEmpty() }?.toJsonString()

        val (photoPath, coverPath) = withContext(Dispatchers.IO) {
            fun store(uri: String?, kind: ProfilePhotoStore.Kind, old: String?): String? =
                if (uri == null) {
                    photos.delete(user.id, kind)
                    null
                } else {
                    photos.save(user.id, uri, kind) ?: old
                }
            store(photoUri, ProfilePhotoStore.Kind.PROFILE, user.photoPath) to
                store(coverUri, ProfilePhotoStore.Kind.COVER, user.coverPath)
        }
        stores.of(user.accountType)
            .updateProfile(id, about, location, latitude, longitude, placesJson, photoPath, coverPath)
        publish(
            user.copy(
                about = about, location = location, latitude = latitude, longitude = longitude, places = places,
                photoPath = photoPath, coverPath = coverPath,
            )
        )
        return true
    }

    override suspend fun savePlaces(places: List<PlaceSelection>): Boolean {
        val user = _currentUser.value ?: return false
        val id = user.id.toLongOrNull() ?: return false
        val main = places.firstOrNull()
        val placesJson = places.takeIf { it.isNotEmpty() }?.toJsonString()
        stores.of(user.accountType).updatePlaces(id, main?.name, main?.latitude, main?.longitude, placesJson)
        publish(user.copy(location = main?.name, latitude = main?.latitude, longitude = main?.longitude, places = places))
        return true
    }

    

    

    
    private suspend fun register(entity: UserEntity, onConflict: suspend () -> AuthResult): AuthResult =
        try {
            startSession(insert(entity), newAccount = true)
        } catch (_: SQLiteConstraintException) {
            onConflict()
        }

    private fun startSession(entity: UserEntity, newAccount: Boolean = false): AuthResult {
        val user = entity.toAuthUser()
        session.save(user)
        if (newAccount) session.markSetupPending(user.id)
        _currentUser.value = user
        return AuthResult.Success(user)
    }

    
    private fun endSession() {
        session.clear()
        flow.clearAccountScoped()
        _currentUser.value = null
    }

    
    private fun publish(updated: AuthUser) {
        session.save(updated)
        _currentUser.value = updated
    }

    private suspend fun findConflict(identifier: String?, username: String?, email: String?): AuthResult.Failure? {
        val directory = stores.directory()
        identifier?.let { directory.byIdentifier(it) }?.let { return identifierTaken(it) }
        username?.let { directory.byUsername(it) }?.let { return AuthResult.Failure(AuthError.USERNAME_TAKEN) }
        email?.let { directory.byEmail(it) }?.let { return AuthResult.Failure(AuthError.EMAIL_TAKEN) }
        return null
    }

    private suspend fun daoFor(entity: UserEntity): UserDao = stores.of(AccountType.fromName(entity.accountType))

    
    private suspend fun insert(entity: UserEntity): UserEntity {
        val withId = entity.copy(id = stores.newId())
        daoFor(withId).insert(withId)
        return withId
    }

    private fun identifierTaken(existing: UserEntity) =
        AuthResult.Failure(AuthError.IDENTIFIER_TAKEN, AccountType.fromName(existing.accountType))

    private fun normalize(identifier: String) = identifier.trim().lowercase()

    

    companion object {
        const val PROVIDER_PASSWORD = "password"

        fun create(context: Context): LocalAuthRepository = LocalAuthRepository(
            stores = AccountStores(context),
            session = SessionManager(context),
            photos = ProfilePhotoStore(context),
            flow = DeviceFlowStore(context),
        )
    }
}