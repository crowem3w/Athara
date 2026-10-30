package social.ahtara.authentication.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
import social.ahtara.authentication.domain.placesFromJson
import social.ahtara.authentication.domain.toJsonString
import social.ahtara.network.ApiClient
import social.ahtara.network.ApiException

/**
 * [AuthRepository] backed by the Ahtara backend.
 *
 * The local Room databases and [SessionManager] stay as an **offline cache**: every successful remote
 * auth mirrors the account into Room (same PBKDF2 hash format), so the app still opens signed-in
 * with the cached profile when the server is unreachable.
 *
 * **Legacy accounts** (created before the backend existed) are imported lazily: when sign-in is
 * rejected by the server but the password matches the local hash, the account is created on the
 * backend with the password the user just typed (the only moment we have the plaintext), the profile
 * is pushed, cached photos are re-keyed to the new id, and the old local row is replaced.
 */
internal class RemoteAuthRepository(
    private val api: ApiClient,
    private val stores: AccountStores,
    private val session: SessionManager,
    private val photos: ProfilePhotoStore,
    private val flow: DeviceFlowStore,
    private val hasher: PasswordHasher = PasswordHasher(),
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUser = MutableStateFlow(initialUser())
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    init {
        api.onSessionExpired = { endSession() }
        _currentUser.value?.let { scope.launch { syncPhotos(it, null) } }
    }

    /** A cached session without tokens is a pre-backend session: sign out so the legacy import can run on sign-in. */
    private fun initialUser(): AuthUser? {
        val cached = session.read() ?: return null
        if (api.tokens.hasSession) return cached
        session.clear()
        return null
    }

    // ------------------------------------------------------------------ sign up / in

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
        val body = JSONObject()
            .put("name", name.trim())
            .put("password", password)
            .put("account_type", accountType.name)
            .put("details", detailsJson(details))
        identifier?.let(::normalize)?.ifBlank { null }?.let { body.put("identifier", it) }
        username?.trim()?.ifBlank { null }?.let { body.put("username", it) }
        email?.let(::normalize)?.ifBlank { null }?.let { body.put("email", it) }
        birthday?.trim()?.ifBlank { null }?.let { body.put("birthday", it) }
        return remote { finishAuth(api.request("POST", "/auth/signup", body = body, auth = false)!!, password, newAccount = true) }
    }

    override suspend fun signIn(identifier: String, password: String): AuthResult {
        val key = normalize(identifier)
        return try {
            val body = JSONObject().put("identifier", key).put("password", password)
            finishAuth(api.request("POST", "/auth/signin", body = body, auth = false)!!, password, newAccount = false)
        } catch (e: ApiException) {
            when {
                e.isNetwork -> AuthResult.Failure(AuthError.NETWORK)
                e.code == "INVALID_CREDENTIALS" -> importLegacy(key, password) ?: AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
                e.code == "TOO_MANY_ATTEMPTS" -> AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
                else -> AuthResult.Failure(AuthError.UNKNOWN)
            }
        }
    }

    /** Null = no matching local account (plain wrong password). */
    private suspend fun importLegacy(key: String, password: String): AuthResult? {
        val local = stores.directory().byAnyId(key) ?: return null
        val stored = local.passwordHash ?: return null
        if (!withContext(Dispatchers.Default) { hasher.verify(password, stored) }) return null

        val body = JSONObject()
            .put("name", local.displayName)
            .put("password", password)
            .put("account_type", local.accountType)
            .put("details", detailsJson(AccountDetails(local.industry, local.phone, local.registrationId)))
        local.identifier?.let { body.put("identifier", it) }
        local.username?.let { body.put("username", it) }
        local.email?.let { body.put("email", it) }
        local.birthday?.let { body.put("birthday", it) }

        return try {
            val auth = api.request("POST", "/auth/signup", body = body, auth = false)!!
            val newId = auth.getJSONObject("account").getString("id")
            withContext(Dispatchers.IO) { photos.move(local.id.toString(), newId) }
            // Account exists on the server now: push what the user had set up locally.
            val patch = profilePatch(
                local.about, local.location, local.latitude, local.longitude, placesFromJson(local.placesJson),
            )
            runCatching { api.request("PATCH", "/users/me", body = patch) }
            if (!session.isSetupPending(local.id.toString())) {
                runCatching { api.request("POST", "/users/me/setup/complete") }
            }
            finishAuth(auth, password, newAccount = false)
        } catch (e: ApiException) {
            when {
                e.isNetwork -> AuthResult.Failure(AuthError.NETWORK)
                // Taken on the server with a different password: the credentials really are wrong.
                e.status == 409 -> AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
                else -> AuthResult.Failure(AuthError.UNKNOWN)
            }
        }
    }

    // ------------------------------------------------------------------ social

    /** The old demo "Apple/Google" button created local-only accounts; with a backend it has no meaning. */
    override suspend fun signInWithSocial(provider: SocialProvider): AuthResult = AuthResult.Failure(AuthError.UNKNOWN)

    override suspend fun continueWithSocial(account: SocialAccount): AuthResult {
        val token = account.idToken ?: return AuthResult.Failure(AuthError.UNKNOWN)
        val body = JSONObject().put("provider", account.provider.name.lowercase()).put("id_token", token)
        return remote { finishAuth(api.request("POST", "/auth/social", body = body, auth = false)!!, null, newAccount = false, social = account) }
    }

    override suspend fun signUpWithSocial(account: SocialAccount, identifier: String, accountType: AccountType): AuthResult {
        val token = account.idToken ?: return AuthResult.Failure(AuthError.UNKNOWN)
        val body = JSONObject()
            .put("provider", account.provider.name.lowercase())
            .put("id_token", token)
            .put("account_type", accountType.name)
            .put("identifier", normalize(identifier))
        return remote { finishAuth(api.request("POST", "/auth/social", body = body, auth = false)!!, null, newAccount = true, social = account) }
    }

    /**
     * The backend resets passwords with an emailed/SMS code (`/auth/password/reset/request` then `/confirm`),
     * but the current screen has no code step. Not wired yet, deliberately: changing only the cached hash
     * would leave the real password unchanged.
     */
    override suspend fun resetPassword(identifier: String, newPassword: String): AuthResult =
        AuthResult.Failure(AuthError.UNKNOWN)

    // ------------------------------------------------------------------ session

    override suspend fun signOut() {
        api.tokens.refresh?.let { refresh ->
            runCatching { api.request("POST", "/auth/signout", body = JSONObject().put("refresh_token", refresh), auth = false) }
        }
        api.tokens.clear()
        endSession()
    }

    override fun isSetupPending(): Boolean = _currentUser.value?.let { session.isSetupPending(it.id) } == true

    override fun completeSetup() {
        session.completeSetup()
        scope.launch { runCatching { api.request("POST", "/users/me/setup/complete") } }
    }

    override suspend fun discardNewAccount(): Boolean {
        val user = _currentUser.value ?: return false
        if (!session.isSetupPending(user.id)) return false
        try {
            api.request("POST", "/auth/account/discard")
        } catch (e: ApiException) {
            return false
        }
        withContext(Dispatchers.IO) { photos.delete(user.id) }
        user.id.toLongOrNull()?.let { runCatching { stores.of(user.accountType).deleteById(it) } }
        api.tokens.clear()
        endSession()
        return true
    }

    // ------------------------------------------------------------------ profile

    override suspend fun savePersonalSetup(setup: PersonalSetup): Boolean = saveProfileSetup(
        setup.about, setup.location, setup.latitude, setup.longitude, setup.places, setup.photoUri, setup.coverUri,
    )

    override suspend fun saveBusinessSetup(setup: BusinessSetup): Boolean = saveProfileSetup(
        setup.about, setup.location, setup.latitude, setup.longitude, setup.places, setup.photoUri, setup.coverUri,
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
        val about = aboutInput.trim().ifBlank { null }
        val main = places.firstOrNull()
        val location = main?.name ?: locationInput.trim().ifBlank { null }
        val latitude = if (location != null) main?.latitude ?: latitudeInput else null
        val longitude = if (location != null) main?.longitude ?: longitudeInput else null

        // Local cache first (same rules as before), then upload; uploads are best-effort.
        val (photoPath, coverPath) = withContext(Dispatchers.IO) {
            fun store(uri: String?, kind: ProfilePhotoStore.Kind, old: String?): String? =
                if (uri == null) { photos.delete(user.id, kind); null } else photos.save(user.id, uri, kind) ?: old
            store(photoUri, ProfilePhotoStore.Kind.PROFILE, user.photoPath) to
                store(coverUri, ProfilePhotoStore.Kind.COVER, user.coverPath)
        }
        val patch = profilePatch(about, location, latitude, longitude, places)
        uploadPhoto(photoPath, ProfilePhotoStore.Kind.PROFILE)?.let { patch.put("photo_media_id", it) }
        uploadPhoto(coverPath, ProfilePhotoStore.Kind.COVER)?.let { patch.put("cover_media_id", it) }
        if (photoPath == null) patch.put("photo_media_id", JSONObject.NULL)
        if (coverPath == null) patch.put("cover_media_id", JSONObject.NULL)

        try {
            api.request("PATCH", "/users/me", body = patch)
        } catch (e: ApiException) {
            return false
        }
        val updated = user.copy(
            about = about, location = location, latitude = latitude, longitude = longitude, places = places,
            photoPath = photoPath, coverPath = coverPath,
        )
        publish(updated)
        mirror(updated, password = null, providerId = null)
        return true
    }

    override suspend fun savePlaces(places: List<PlaceSelection>): Boolean {
        val user = _currentUser.value ?: return false
        val main = places.firstOrNull()
        val patch = JSONObject()
            .put("location", main?.name ?: JSONObject.NULL)
            .put("latitude", main?.latitude ?: JSONObject.NULL)
            .put("longitude", main?.longitude ?: JSONObject.NULL)
            .put("places", placesJson(places))
        try {
            api.request("PATCH", "/users/me", body = patch)
        } catch (e: ApiException) {
            return false
        }
        val updated = user.copy(location = main?.name, latitude = main?.latitude, longitude = main?.longitude, places = places)
        publish(updated)
        mirror(updated, password = null, providerId = null)
        return true
    }

    // ------------------------------------------------------------------ photos (storage may not exist yet)

    /** Uploads a cached photo; returns the media id, or null on any failure (e.g. no object storage yet). */
    private suspend fun uploadPhoto(path: String?, kind: ProfilePhotoStore.Kind): String? {
        path ?: return null
        return try {
            val bytes = withContext(Dispatchers.IO) { File(path).readBytes() }
            val kindName = if (kind == ProfilePhotoStore.Kind.PROFILE) "profile" else "cover"
            api.upload("/media/images", mapOf("kind" to kindName), bytes, File(path).name)
                ?.optString("id")?.ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    /** Retries uploads of cached photos the server doesn't have yet. */
    private suspend fun syncPhotos(user: AuthUser, profile: JSONObject?) {
        runCatching {
            val p = profile ?: api.request("GET", "/users/me") ?: return
            val patch = JSONObject()
            for ((kind, urlKey, field) in listOf(
                Triple(ProfilePhotoStore.Kind.PROFILE, "photo_url", "photo_media_id"),
                Triple(ProfilePhotoStore.Kind.COVER, "cover_url", "cover_media_id"),
            )) {
                if (!p.isNull(urlKey)) continue
                uploadPhoto(photos.pathOf(user.id, kind), kind)?.let { patch.put(field, it) }
            }
            if (patch.length() > 0) api.request("PATCH", "/users/me", body = patch)
        }
    }

    // ------------------------------------------------------------------ plumbing

    private suspend fun remote(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (e: ApiException) {
        AuthResult.Failure(mapError(e), e.extra?.optStringOrNull("existing_type")?.let(AccountType::fromName))
    }

    private fun mapError(e: ApiException): AuthError = when {
        e.isNetwork -> AuthError.NETWORK
        e.code == "IDENTIFIER_TAKEN" -> AuthError.IDENTIFIER_TAKEN
        e.code == "USERNAME_TAKEN" -> AuthError.USERNAME_TAKEN
        e.code == "EMAIL_TAKEN" -> AuthError.EMAIL_TAKEN
        e.code == "ACCOUNT_NOT_FOUND" -> AuthError.ACCOUNT_NOT_FOUND
        e.code == "INVALID_CREDENTIALS" || e.code == "TOO_MANY_ATTEMPTS" -> AuthError.INVALID_CREDENTIALS
        else -> AuthError.UNKNOWN
    }

    /** Stores tokens, loads the profile, refreshes the Room cache and starts the local session. */
    private suspend fun finishAuth(
        auth: JSONObject,
        password: String?,
        newAccount: Boolean,
        social: SocialAccount? = null,
    ): AuthResult {
        api.tokens.save(auth.getString("access_token"), auth.getString("refresh_token"), auth.optLong("expires_in", 900))
        val account = auth.getJSONObject("account")
        val profile = runCatching { api.request("GET", "/users/me") }.getOrNull()
        val user = buildUser(account, profile)
        mirror(user, password, social?.subject, account.optStringOrNull("identifier"))
        val pending = newAccount || auth.optBoolean("is_new") || profile?.optBoolean("setup_completed", true) == false
        session.save(user)
        if (pending) session.markSetupPending(user.id)
        _currentUser.value = user
        scope.launch { syncPhotos(user, profile) }
        return AuthResult.Success(user)
    }

    private suspend fun buildUser(account: JSONObject, profile: JSONObject?): AuthUser {
        val id = account.getString("id")
        val type = AccountType.fromName(account.getString("account_type"))
        val username = account.optStringOrNull("username")
        val email = account.optStringOrNull("email")
        val identifier = account.optStringOrNull("identifier")
        val cached = id.toLongOrNull()?.let { runCatching { stores.of(type).findById(it) }.getOrNull() }
        val places = profile?.optJSONArray("places")?.let(::placesFromArray) ?: placesFromJson(cached?.placesJson)
        return AuthUser(
            id = id,
            identifier = username ?: identifier ?: email.orEmpty(),
            displayName = profile?.optStringOrNull("display_name") ?: cached?.displayName ?: (username ?: identifier ?: email).orEmpty(),
            provider = if (account.optString("provider") == "local") LocalAuthRepository.PROVIDER_PASSWORD else account.optString("provider"),
            accountType = type,
            details = AccountDetails(
                industry = profile?.optStringOrNull("industry") ?: cached?.industry,
                phone = profile?.optStringOrNull("phone") ?: cached?.phone,
                registrationId = profile?.optStringOrNull("registration_id") ?: cached?.registrationId,
            ),
            username = username,
            email = email,
            birthday = profile?.optStringOrNull("birthday") ?: cached?.birthday,
            about = profile?.optStringOrNull("about") ?: cached?.about,
            location = profile?.optStringOrNull("location") ?: cached?.location,
            latitude = profile?.optDoubleOrNull("latitude") ?: cached?.latitude,
            longitude = profile?.optDoubleOrNull("longitude") ?: cached?.longitude,
            places = places,
            photoPath = photos.pathOf(id, ProfilePhotoStore.Kind.PROFILE),
            coverPath = photos.pathOf(id, ProfilePhotoStore.Kind.COVER),
        )
    }

    /**
     * Upserts the account into the local Room database under its backend id, replacing any older local
     * row for the same person (the legacy import). Best-effort: a cache failure never fails auth.
     */
    private suspend fun mirror(user: AuthUser, password: String?, providerId: String?, identifier: String? = null) {
        runCatching {
            val id = user.id.toLongOrNull() ?: return
            val dao = stores.of(user.accountType)
            val keys = listOfNotNull(user.username?.lowercase(), user.email?.lowercase(), user.identifier.lowercase().ifBlank { null })
            // Drop stale rows for the same person (legacy ids differ from backend ids).
            for (key in keys) {
                stores.directory().byAnyId(key)?.let { old ->
                    if (old.id != id) stores.of(AccountType.fromName(old.accountType)).deleteById(old.id)
                }
            }
            val prev = dao.findById(id)
            val hash = if (password != null) withContext(Dispatchers.Default) { hasher.hash(password) } else prev?.passwordHash
            prev?.let { dao.deleteById(id) }
            dao.insert(
                UserEntity(
                    id = id,
                    identifier = identifier ?: prev?.identifier,
                    displayName = user.displayName,
                    passwordHash = hash,
                    provider = user.provider,
                    createdAt = prev?.createdAt ?: System.currentTimeMillis(),
                    accountType = user.accountType.name,
                    industry = user.details.industry,
                    phone = user.details.phone,
                    registrationId = user.details.registrationId,
                    providerId = providerId ?: prev?.providerId,
                    username = user.username?.lowercase(),
                    email = user.email?.lowercase(),
                    birthday = user.birthday,
                    about = user.about,
                    location = user.location,
                    photoPath = user.photoPath,
                    coverPath = user.coverPath,
                    latitude = user.latitude,
                    longitude = user.longitude,
                    placesJson = user.places.takeIf { it.isNotEmpty() }?.toJsonString(),
                )
            )
        }
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

    private fun profilePatch(about: String?, location: String?, lat: Double?, lng: Double?, places: List<PlaceSelection>) =
        JSONObject()
            .put("about", about.orEmpty())
            .put("location", location ?: JSONObject.NULL)
            .put("latitude", lat ?: JSONObject.NULL)
            .put("longitude", lng ?: JSONObject.NULL)
            .put("places", placesJson(places))

    private fun placesJson(places: List<PlaceSelection>) = JSONArray().also { arr ->
        places.forEach { arr.put(JSONObject().put("name", it.name).put("lat", it.latitude).put("lng", it.longitude)) }
    }

    private fun placesFromArray(array: JSONArray): List<PlaceSelection> = (0 until array.length()).mapNotNull { i ->
        array.optJSONObject(i)?.let { o ->
            val name = o.optString("name").takeIf { it.isNotBlank() } ?: return@let null
            PlaceSelection(name, o.optDouble("lat"), o.optDouble("lng"))
        }
    }

    private fun detailsJson(d: AccountDetails) = JSONObject().also {
        d.industry?.trim()?.ifBlank { null }?.let { v -> it.put("industry", v) }
        d.phone?.trim()?.ifBlank { null }?.let { v -> it.put("phone", v) }
        d.registrationId?.trim()?.ifBlank { null }?.let { v -> it.put("registration_id", v) }
    }

    private fun normalize(identifier: String) = identifier.trim().lowercase()

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).ifBlank { null }
    private fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key)) null else optDouble(key)

    companion object {
        fun create(context: Context): RemoteAuthRepository {
            val app = context.applicationContext
            return RemoteAuthRepository(
                api = ApiClient.get(app),
                stores = AccountStores(app),
                session = SessionManager(app),
                photos = ProfilePhotoStore(app),
                flow = DeviceFlowStore(app),
            )
        }
    }
}
