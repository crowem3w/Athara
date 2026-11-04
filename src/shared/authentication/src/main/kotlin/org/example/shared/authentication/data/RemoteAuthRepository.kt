package org.example.shared.authentication.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import org.example.core.identityapi.AccountDetails
import org.example.core.identityapi.AccountType
import org.example.core.identityapi.AuthError
import org.example.core.identityapi.AuthRepository
import org.example.core.identityapi.AuthResult
import org.example.core.identityapi.AuthUser
import org.example.core.identityapi.PersonalSetup
import org.example.core.identityapi.SocialAccount
import org.example.core.identityapi.SocialProvider
import org.example.core.network.Core
import org.example.core.network.ApiResult
import org.example.core.network.apiCall
import org.example.core.securestorage.TokenStore






internal class RemoteAuthRepository(
    private val context: Context,
    private val api: AuthApi,
    private val http: OkHttpClient,
    private val tokens: TokenStore,
    private val session: SessionStore,
    private val photos: ProfilePhotoStore,
    private val proofs: PhoneProofStore,
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentUser = MutableStateFlow(restore())
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    init {
        
        scope.launch {
            tokens.signedIn.collect { signedIn -> if (!signedIn && _currentUser.value != null) clearLocal() }
        }
        
        if (_currentUser.value != null) scope.launch { refreshProfile() }
    }

    

    override suspend fun checkAvailable(field: String, value: String): Boolean? =
        when (val result = apiCall { api.validate(ValidateRequest(field, value.trim())) }) {
            is ApiResult.Ok -> when {
                result.value.valid -> true
                result.value.code == "TAKEN" -> false
                else -> null 
            }
            is ApiResult.Failure -> null
        }

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
        val request = SignUpRequest(
            name = name.trim(),
            identifier = identifier?.let(::normalize)?.ifBlank { null },
            password = password,
            accountType = accountType.name,
            details = DetailsDto(
                industry = details.industry?.trim()?.ifBlank { null },
                phone = details.phone?.trim()?.ifBlank { null },
                registrationId = details.registrationId?.trim()?.ifBlank { null },
            ).takeIf { it.industry != null || it.phone != null || it.registrationId != null },
            username = username?.let(::normalize)?.ifBlank { null },
            email = email?.let(::normalize)?.ifBlank { null },
            birthday = birthday?.trim()?.ifBlank { null },
            phoneVerificationToken = proofs.consume(),
        )
        return startSession(apiCall { api.signUp(request) }, newAccount = true)
    }

    override suspend fun signIn(identifier: String, password: String): AuthResult =
        startSession(apiCall { api.signIn(SignInRequest(normalize(identifier), password)) })

    
    override suspend fun signInWithSocial(provider: SocialProvider): AuthResult =
        AuthResult.Failure(AuthError.UNKNOWN)

    override suspend fun continueWithSocial(account: SocialAccount): AuthResult {
        val idToken = account.idToken ?: return AuthResult.Failure(AuthError.UNKNOWN)
        return startSession(apiCall { api.socialLogin(SocialLoginRequest(account.provider.wire(), idToken)) })
    }

    override suspend fun signUpWithSocial(account: SocialAccount, identifier: String, accountType: AccountType): AuthResult {
        val idToken = account.idToken ?: return AuthResult.Failure(AuthError.UNKNOWN)
        val request = SocialSignUpRequest(
            provider = account.provider.wire(),
            idToken = idToken,
            identifier = normalize(identifier),
            accountType = accountType.name,
            phoneVerificationToken = proofs.consume(),
        )
        return startSession(apiCall { api.socialSignUp(request) }, newAccount = true)
    }

    
    override suspend fun resetPassword(identifier: String, newPassword: String): AuthResult =
        when (val result = apiCall {
            api.resetPassword(ResetPasswordRequest(normalize(identifier), newPassword, proofs.consume()))
        }) {
            is ApiResult.Ok -> AuthResult.Success(result.value.toAuthUser(null, null))
            is ApiResult.Failure -> result.toFailure()
        }

    

    override suspend fun signOut() {
        val refresh = tokens.refreshToken
        if (refresh != null) {
            
            withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { apiCall { api.logout(LogoutRequest(refresh)) } }
        }
        clearLocal()
    }

    override fun isSetupPending(): Boolean =
        _currentUser.value?.let { session.isSetupPending(it.id) } == true

    override fun completeSetup() = session.completeSetup()

    
    override suspend fun discardNewAccount(): Boolean {
        val user = _currentUser.value ?: return false
        if (!session.isSetupPending(user.id)) return false
        val result = apiCall { api.deleteMe() }
        if (result is ApiResult.Failure && result.httpCode != 404) return false
        clearLocal()
        return true
    }

    override suspend fun savePersonalSetup(setup: PersonalSetup): Boolean {
        val user = _currentUser.value ?: return false
        val about = setup.about.trim().ifBlank { null }
        val location = setup.location.trim().ifBlank { null }

        if (apiCall { api.updateMe(ProfileUpdateRequest(about, location)) } !is ApiResult.Ok) return false

        val photoPath = syncPhoto(user, ProfilePhotoStore.Kind.PROFILE, setup.photoUri, user.photoPath)
        val coverPath = syncPhoto(user, ProfilePhotoStore.Kind.COVER, setup.coverUri, user.coverPath)
        if (photoPath is PhotoResult.Failed || coverPath is PhotoResult.Failed) return false

        val updated = user.copy(
            about = about,
            location = location,
            photoPath = (photoPath as PhotoResult.Done).path,
            coverPath = (coverPath as PhotoResult.Done).path,
        )
        session.save(updated)
        _currentUser.value = updated
        return true
    }

    

    private sealed interface PhotoResult {
        data class Done(val path: String?) : PhotoResult
        data object Failed : PhotoResult
    }

    
    private suspend fun syncPhoto(user: AuthUser, kind: ProfilePhotoStore.Kind, uri: String?, old: String?): PhotoResult {
        val segment = kind.name.lowercase()
        if (uri == null) {
            withContext(Dispatchers.IO) { photos.delete(user.id, kind) }
            val result = apiCall { api.deletePhoto(segment) }
            return if (result is ApiResult.Failure && result.httpCode != 404) PhotoResult.Failed else PhotoResult.Done(null)
        }
        val path = withContext(Dispatchers.IO) { photos.save(user.id, uri, kind) } ?: return PhotoResult.Done(old) 
        val part = MultipartBody.Part.createFormData("file", java.io.File(path).name, java.io.File(path).asRequestBody("image/jpeg".toMediaType()))
        return if (apiCall { api.uploadPhoto(segment, part) } is ApiResult.Ok) PhotoResult.Done(path) else PhotoResult.Failed
    }

    
    private suspend fun startSession(result: ApiResult<TokenResponse>, newAccount: Boolean = false): AuthResult {
        if (result is ApiResult.Failure) return result.toFailure()
        val body = (result as ApiResult.Ok).value
        val dto = body.user ?: run {
            
            tokens.save(TokenStore.Tokens(body.accessToken, body.refreshToken, userId = ""))
            when (val me = apiCall { api.me() }) {
                is ApiResult.Ok -> me.value
                is ApiResult.Failure -> { tokens.clear(); return me.toFailure() }
            }
        }
        tokens.save(TokenStore.Tokens(body.accessToken, body.refreshToken, dto.id))
        val user = dto.toAuthUser(
            photos.existingPath(dto.id, ProfilePhotoStore.Kind.PROFILE),
            photos.existingPath(dto.id, ProfilePhotoStore.Kind.COVER),
        )
        session.save(user)
        
        if (newAccount) session.markSetupPending(user.id)
        _currentUser.value = user
        scope.launch { hydratePhotos(dto) }
        return AuthResult.Success(user)
    }

    
    private fun restore(): AuthUser? {
        legacyCleanup()
        val user = if (tokens.signedIn.value) session.read() else null
        if (user == null) {
            if (tokens.signedIn.value) tokens.clear()
            session.clear()
        }
        return user
    }

    private suspend fun refreshProfile() {
        val result = apiCall { api.me() } 
        if (result !is ApiResult.Ok) return
        val dto = result.value
        val current = _currentUser.value?.takeIf { it.id == dto.id } ?: return
        val user = dto.toAuthUser(current.photoPath, current.coverPath)
        session.save(user)
        _currentUser.value = user
        hydratePhotos(dto)
    }

    
    private suspend fun hydratePhotos(dto: UserDto) = withContext(Dispatchers.IO) {
        fun fetch(url: String?, kind: ProfilePhotoStore.Kind): String? =
            if (url == null) { photos.delete(dto.id, kind); null }
            else photos.download(dto.id, url, kind, http) ?: photos.existingPath(dto.id, kind)
        val profile = fetch(dto.photoUrl, ProfilePhotoStore.Kind.PROFILE)
        val cover = fetch(dto.coverUrl, ProfilePhotoStore.Kind.COVER)
        val current = _currentUser.value?.takeIf { it.id == dto.id } ?: return@withContext
        if (current.photoPath != profile || current.coverPath != cover) {
            val updated = current.copy(photoPath = profile, coverPath = cover)
            session.save(updated)
            _currentUser.value = updated
        }
    }

    private fun clearLocal() {
        val userId = _currentUser.value?.id
        tokens.clear()
        session.clear()
        _currentUser.value = null
        if (userId != null) photos.delete(userId) 
    }

    
    private fun legacyCleanup() {
        if (!context.getDatabasePath(LEGACY_DB).exists()) return
        context.deleteDatabase(LEGACY_DB)
        context.deleteSharedPreferences(LEGACY_PREFS)
        photos.deleteAll()
    }

    private fun ApiResult.Failure.toFailure(): AuthResult.Failure {
        val error = when {
            network -> AuthError.NETWORK
            code == "INVALID_CREDENTIALS" || httpCode == 401 -> AuthError.INVALID_CREDENTIALS
            code == "IDENTIFIER_TAKEN" -> AuthError.IDENTIFIER_TAKEN
            code == "USERNAME_TAKEN" -> AuthError.USERNAME_TAKEN
            code == "EMAIL_TAKEN" -> AuthError.EMAIL_TAKEN
            code == "ACCOUNT_NOT_FOUND" || httpCode == 404 -> AuthError.ACCOUNT_NOT_FOUND
            code == "VERIFICATION_REQUIRED" -> AuthError.VERIFICATION_REQUIRED
            code == "INVALID_REQUEST" -> AuthError.INVALID_INPUT
            else -> AuthError.UNKNOWN
        }
        return AuthResult.Failure(error, existingType = existingType?.let { AccountType.fromName(it) }, fieldCodes = fieldErrors)
    }

    private fun UserDto.toAuthUser(photoPath: String?, coverPath: String?) = AuthUser(
        id = id,
        
        identifier = username ?: identifier ?: email.orEmpty(),
        displayName = displayName,
        provider = provider,
        accountType = AccountType.fromName(accountType),
        details = AccountDetails(industry = industry, phone = phone, registrationId = registrationId),
        username = username,
        email = email,
        birthday = birthday,
        about = about,
        location = location,
        photoPath = photoPath,
        coverPath = coverPath,
    )

    private fun SocialProvider.wire() = name.lowercase()

    private fun normalize(identifier: String) = identifier.trim().lowercase()

    companion object {
        private const val LOGOUT_TIMEOUT_MS = 3_000L
        private const val LEGACY_DB = "authentication.db"
        private const val LEGACY_PREFS = "authentication_session"

        fun create(context: Context, proofs: PhoneProofStore): RemoteAuthRepository = RemoteAuthRepository(
            context = context,
            api = Core.create(AuthApi::class.java),
            http = Core.http,
            tokens = Core.tokens,
            session = SessionStore(Core.secure),
            photos = ProfilePhotoStore(context),
            proofs = proofs,
        )
    }
}