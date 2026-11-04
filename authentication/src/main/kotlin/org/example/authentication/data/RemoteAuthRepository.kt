package org.example.authentication.data

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
import org.example.identityapi.AccountDetails
import org.example.identityapi.AccountType
import org.example.identityapi.AuthError
import org.example.identityapi.AuthRepository
import org.example.identityapi.AuthResult
import org.example.identityapi.AuthUser
import org.example.identityapi.PersonalSetup
import org.example.identityapi.SocialAccount
import org.example.identityapi.SocialProvider
import org.example.network.Core
import org.example.network.ApiResult
import org.example.network.apiCall
import org.example.securestorage.TokenStore

/**
 * Backend implementation: the server owns accounts and passwords; the device holds only tokens
 * (Keystore-encrypted) and a cached copy of the profile. Server data always wins: the profile is
 * re-fetched on start, and an expired/revoked session signs the user out automatically.
 */
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
        // Refresh token rejected (expired, revoked, password changed elsewhere): back to sign-in.
        scope.launch {
            tokens.signedIn.collect { signedIn -> if (!signedIn && _currentUser.value != null) clearLocal() }
        }
        // Server is authoritative: pick up profile changes made elsewhere.
        if (_currentUser.value != null) scope.launch { refreshProfile() }
    }

    // ---- sign up / sign in ------------------------------------------------------------------

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

    /** Demo social accounts are gone; real providers go through [continueWithSocial] with an ID token. */
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

    /** The backend only accepts this together with a fresh OTP proof (see [PhoneProofStore]); it does not sign in. */
    override suspend fun resetPassword(identifier: String, newPassword: String): AuthResult =
        when (val result = apiCall {
            api.resetPassword(ResetPasswordRequest(normalize(identifier), newPassword, proofs.consume()))
        }) {
            is ApiResult.Ok -> AuthResult.Success(result.value.toAuthUser(null, null))
            is ApiResult.Failure -> result.toFailure()
        }

    // ---- session ----------------------------------------------------------------------------

    override suspend fun signOut() {
        val refresh = tokens.refreshToken
        if (refresh != null) {
            // Best effort: revoke server-side, but never keep the user stuck on a bad connection.
            withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { apiCall { api.logout(LogoutRequest(refresh)) } }
        }
        clearLocal()
    }

    override fun isSetupPending(): Boolean =
        _currentUser.value?.let { session.isSetupPending(it.id) } == true

    override fun completeSetup() = session.completeSetup()

    /** Deletes the just-created account on the server, then signs out. False (nothing changed) if that fails. */
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

    // ---- internals --------------------------------------------------------------------------

    private sealed interface PhotoResult {
        data class Done(val path: String?) : PhotoResult
        data object Failed : PhotoResult
    }

    /** Stores the picked image locally and mirrors it (or its removal) to the server. */
    private suspend fun syncPhoto(user: AuthUser, kind: ProfilePhotoStore.Kind, uri: String?, old: String?): PhotoResult {
        val segment = kind.name.lowercase()
        if (uri == null) {
            withContext(Dispatchers.IO) { photos.delete(user.id, kind) }
            val result = apiCall { api.deletePhoto(segment) }
            return if (result is ApiResult.Failure && result.httpCode != 404) PhotoResult.Failed else PhotoResult.Done(null)
        }
        val path = withContext(Dispatchers.IO) { photos.save(user.id, uri, kind) } ?: return PhotoResult.Done(old) // unreadable: keep old
        val part = MultipartBody.Part.createFormData("file", java.io.File(path).name, java.io.File(path).asRequestBody("image/jpeg".toMediaType()))
        return if (apiCall { api.uploadPhoto(segment, part) } is ApiResult.Ok) PhotoResult.Done(path) else PhotoResult.Failed
    }

    /** Turns a token response into a signed-in session. [newAccount]: the account still needs the setup step. */
    private suspend fun startSession(result: ApiResult<TokenResponse>, newAccount: Boolean = false): AuthResult {
        if (result is ApiResult.Failure) return result.toFailure()
        val body = (result as ApiResult.Ok).value
        val dto = body.user ?: run {
            // Contract says the user comes with the tokens; fetch it if a server omits it.
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
        // Set before _currentUser changes: observers open the account's screen as soon as it does.
        if (newAccount) session.markSetupPending(user.id)
        _currentUser.value = user
        scope.launch { hydratePhotos(dto) }
        return AuthResult.Success(user)
    }

    /** Signed in only if both tokens and the cached profile survived (e.g. not after an unreadable keystore entry). */
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
        val result = apiCall { api.me() } // a 401 here refreshes the token or, if rejected, signs out via TokenStore
        if (result !is ApiResult.Ok) return
        val dto = result.value
        val current = _currentUser.value?.takeIf { it.id == dto.id } ?: return
        val user = dto.toAuthUser(current.photoPath, current.coverPath)
        session.save(user)
        _currentUser.value = user
        hydratePhotos(dto)
    }

    /** Downloads the server's pictures (new device / changed elsewhere); removes local ones the server no longer has. */
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
        if (userId != null) photos.delete(userId) // private pictures do not stay on a signed-out device
    }

    /** One-time removal of the old on-device accounts (password hashes, plain-text session, photos). */
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
            else -> AuthError.UNKNOWN
        }
        return AuthResult.Failure(error, existingType = existingType?.let { AccountType.fromName(it) })
    }

    private fun UserDto.toAuthUser(photoPath: String?, coverPath: String?) = AuthUser(
        id = id,
        // What the profile screens show as "Login": username first, then mobile number, then email.
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
