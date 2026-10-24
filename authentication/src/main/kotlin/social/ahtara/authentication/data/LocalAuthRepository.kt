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
import social.ahtara.authentication.domain.PersonalSetup
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.toJsonString
import social.ahtara.authentication.domain.SocialAccount
import social.ahtara.authentication.domain.SocialProvider


internal class LocalAuthRepository(
    private val dao: UserDao,
    private val session: SessionStore,
    private val photos: ProfilePhotoStore,
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
        return try {
            val id = dao.insert(entity)
            startSession(entity.copy(id = id), newAccount = true)
        } catch (_: SQLiteConstraintException) {
            findConflict(key, handle, mail) ?: AuthResult.Failure(AuthError.IDENTIFIER_TAKEN)
        }
    }

    override suspend fun signIn(identifier: String, password: String): AuthResult {
        val user = dao.findByAnyId(normalize(identifier))
        val stored = user?.passwordHash
        
        if (user == null || stored == null) return AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        val ok = withContext(Dispatchers.Default) { hasher.verify(password, stored) }
        return if (ok) startSession(user) else AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
    }

    
    override suspend fun signInWithSocial(provider: SocialProvider): AuthResult {
        val key = "demo.${provider.name.lowercase()}"
        val existing = dao.findByIdentifier(key)
        if (existing != null) return startSession(existing)
        val entity = UserEntity(
            identifier = key,
            displayName = "Demo ${provider.name.lowercase().replaceFirstChar { it.uppercase() }} User",
            passwordHash = null,
            provider = provider.name.lowercase(),
            createdAt = System.currentTimeMillis(),
        )
        return try {
            startSession(entity.copy(id = dao.insert(entity)), newAccount = true)
        } catch (_: SQLiteConstraintException) {
            dao.findByIdentifier(key)?.let { startSession(it) } ?: AuthResult.Failure(AuthError.UNKNOWN)
        }
    }

    override suspend fun continueWithSocial(account: SocialAccount): AuthResult {
        val user = dao.findByProvider(account.provider.name.lowercase(), account.subject)
            ?: return AuthResult.Failure(AuthError.ACCOUNT_NOT_FOUND)
        return startSession(user)
    }

    override suspend fun signUpWithSocial(
        account: SocialAccount,
        identifier: String,
        accountType: AccountType,
    ): AuthResult {
        val key = normalize(identifier)
        dao.findByIdentifier(key)?.let { return identifierTaken(it) }
        val entity = UserEntity(
            identifier = key,
            displayName = account.displayName.trim().ifBlank { account.email.orEmpty() },
            passwordHash = null,
            provider = account.provider.name.lowercase(),
            createdAt = System.currentTimeMillis(),
            accountType = accountType.name,
            providerId = account.subject,
        )
        return try {
            startSession(entity.copy(id = dao.insert(entity)), newAccount = true)
        } catch (_: SQLiteConstraintException) {
            dao.findByIdentifier(key)?.let { identifierTaken(it) } ?: AuthResult.Failure(AuthError.IDENTIFIER_TAKEN)
        }
    }

    override suspend fun resetPassword(identifier: String, newPassword: String): AuthResult {
        val user = dao.findByAnyId(normalize(identifier))
            ?: return AuthResult.Failure(AuthError.ACCOUNT_NOT_FOUND)
        val hash = withContext(Dispatchers.Default) { hasher.hash(newPassword) }
        dao.updatePasswordHash(user.id, hash)
        return AuthResult.Success(user.toAuthUser()) 
    }

    override suspend fun signOut() {
        session.clear()
        _currentUser.value = null
    }

    override fun isSetupPending(): Boolean =
        _currentUser.value?.let { session.isSetupPending(it.id) } == true

    override fun completeSetup() = session.completeSetup()

    override suspend fun discardNewAccount(): Boolean {
        val user = _currentUser.value ?: return false
        val id = user.id.toLongOrNull() ?: return false
        if (!session.isSetupPending(user.id)) return false
        withContext(Dispatchers.IO) { photos.delete(user.id) }
        dao.deleteById(id)
        session.clear()
        _currentUser.value = null
        return true
    }

    override suspend fun savePersonalSetup(setup: PersonalSetup): Boolean {
        val user = _currentUser.value ?: return false
        val id = user.id.toLongOrNull() ?: return false
        val about = setup.about.trim().ifBlank { null }
        
        val main = setup.places.firstOrNull()
        val location = main?.name ?: setup.location.trim().ifBlank { null }
        
        val latitude = if (location != null) main?.latitude ?: setup.latitude else null
        val longitude = if (location != null) main?.longitude ?: setup.longitude else null
        val places = setup.places
        val placesJson = places.takeIf { it.isNotEmpty() }?.toJsonString()
        val (photoPath, coverPath) = withContext(Dispatchers.IO) {
            fun store(uri: String?, kind: ProfilePhotoStore.Kind, old: String?): String? =
                if (uri == null) {
                    photos.delete(user.id, kind)
                    null
                } else {
                    photos.save(user.id, uri, kind) ?: old 
                }
            store(setup.photoUri, ProfilePhotoStore.Kind.PROFILE, user.photoPath) to
                store(setup.coverUri, ProfilePhotoStore.Kind.COVER, user.coverPath)
        }
        dao.updateProfile(id, about, location, latitude, longitude, placesJson, photoPath, coverPath)
        val updated = user.copy(
            about = about, location = location, latitude = latitude, longitude = longitude, places = places,
            photoPath = photoPath, coverPath = coverPath,
        )
        session.save(updated)
        _currentUser.value = updated
        return true
    }

    override suspend fun savePlaces(places: List<PlaceSelection>): Boolean {
        val user = _currentUser.value ?: return false
        val id = user.id.toLongOrNull() ?: return false
        
        val main = places.firstOrNull()
        val placesJson = places.takeIf { it.isNotEmpty() }?.toJsonString()
        dao.updatePlaces(id, main?.name, main?.latitude, main?.longitude, placesJson)
        val updated = user.copy(location = main?.name, latitude = main?.latitude, longitude = main?.longitude, places = places)
        session.save(updated)
        _currentUser.value = updated
        return true
    }

    
    private fun startSession(entity: UserEntity, newAccount: Boolean = false): AuthResult {
        val user = entity.toAuthUser()
        session.save(user)
        
        if (newAccount) session.markSetupPending(user.id)
        _currentUser.value = user
        return AuthResult.Success(user)
    }

    
    private suspend fun findConflict(identifier: String?, username: String?, email: String?): AuthResult.Failure? {
        identifier?.let { dao.findByIdentifier(it) }?.let { return identifierTaken(it) }
        username?.let { dao.findByUsername(it) }?.let { return AuthResult.Failure(AuthError.USERNAME_TAKEN) }
        email?.let { dao.findByEmail(it) }?.let { return AuthResult.Failure(AuthError.EMAIL_TAKEN) }
        return null
    }

    private fun identifierTaken(existing: UserEntity) =
        AuthResult.Failure(AuthError.IDENTIFIER_TAKEN, AccountType.fromName(existing.accountType))

    private fun normalize(identifier: String) = identifier.trim().lowercase()

    companion object {
        const val PROVIDER_PASSWORD = "password"

        fun create(context: Context): LocalAuthRepository = LocalAuthRepository(
            dao = AuthDatabase.create(context).userDao(),
            session = SessionStore(context),
            photos = ProfilePhotoStore(context),
        )
    }
}