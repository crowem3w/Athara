package social.ahtara.authentication.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import social.ahtara.authentication.Authentication
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
import social.ahtara.authentication.net.ApiClient
import social.ahtara.authentication.net.ApiException

/**
 * [AuthRepository] backed by the Ahtara gateway (`/auth`, `/users`, `/media`).
 *
 * The server is the source of truth. The signed-in profile and the "setup pending" marker are cached
 * in [SessionManager] only so the app can start offline; they are refreshed on every sign-in/sign-up
 * and after every profile write.
 */
internal class RemoteAuthRepository(
    private val context: Context,
    private val api: ApiClient,
    private val session: SessionManager,
    private val flow: DeviceFlowStore,
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // A cached profile without tokens is stale (e.g. app data restored without the token prefs).
    private val _currentUser = MutableStateFlow(session.read()?.takeIf { api.tokens.hasSession })
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    init {
        api.onSessionExpired = { scope.launch { endSession() } }
        if (_currentUser.value == null && !api.tokens.hasSession) session.clear()
        // Pick up server-side changes (and setup state) without blocking startup.
        if (_currentUser.value != null) scope.launch { runCatching { refreshProfile() } }
    }

    // --------------------------------------------------------------------------------- sign up / in

    override suspend fun signUp(
        name: String,
        identifier: String?,
        password: String,
        accountType: AccountType,
        details: AccountDetails,
        username: String?,
        email: String?,
        birthday: String?,
    ): AuthResult = guarded {
        val body = JSONObject()
            .put("name", name.trim())
            .put("password", password)
            .put("account_type", accountType.name)
            .put("details", JSONObject()
                .putOpt("industry", details.industry?.trim()?.ifBlank { null })
                .putOpt("phone", details.phone?.trim()?.ifBlank { null })
                .putOpt("registration_id", details.registrationId?.trim()?.ifBlank { null }))
        identifier?.trim()?.ifBlank { null }?.let { body.put("identifier", it) }
        username?.trim()?.ifBlank { null }?.let { body.put("username", it) }
        email?.trim()?.ifBlank { null }?.let { body.put("email", it) }
        birthday?.trim()?.ifBlank { null }?.let { body.put("birthday", it) }
        Authentication.phoneVerifier.let { (it as? RemotePhoneVerifier)?.takeToken(identifier) }
            ?.let { body.put("verification_token", it) }
        startSession(api.post("/auth/signup", body, auth = false).obj())
    }

    override suspend fun signIn(identifier: String, password: String): AuthResult = guarded {
        val body = JSONObject().put("identifier", identifier.trim()).put("password", password)
        startSession(api.post("/auth/signin", body, auth = false).obj())
    }

    /** The old offline demo buttons no longer exist; real Google goes through [continueWithSocial]. */
    override suspend fun signInWithSocial(provider: SocialProvider): AuthResult =
        AuthResult.Failure(AuthError.UNKNOWN)

    override suspend fun continueWithSocial(account: SocialAccount): AuthResult = guarded {
        val token = account.idToken ?: return@guarded AuthResult.Failure(AuthError.UNKNOWN)
        val body = JSONObject().put("provider", account.provider.name.lowercase()).put("id_token", token)
        startSession(api.post("/auth/social", body, auth = false).obj())
    }

    override suspend fun signUpWithSocial(
        account: SocialAccount,
        identifier: String,
        accountType: AccountType,
    ): AuthResult = guarded {
        val token = account.idToken ?: return@guarded AuthResult.Failure(AuthError.UNKNOWN)
        val body = JSONObject()
            .put("provider", account.provider.name.lowercase())
            .put("id_token", token)
            .put("account_type", accountType.name)
            .put("identifier", identifier.trim())
        startSession(api.post("/auth/social", body, auth = false).obj())
    }

    override suspend fun requestPasswordReset(identifier: String): Boolean = try {
        api.post("/auth/password/reset/request", JSONObject().put("identifier", identifier.trim()), auth = false)
        true
    } catch (_: ApiException) {
        false
    }

    override suspend fun resetPassword(identifier: String, code: String, newPassword: String): AuthResult = guarded {
        api.post(
            "/auth/password/reset/confirm",
            JSONObject().put("identifier", identifier.trim()).put("code", code.trim()).put("new_password", newPassword),
            auth = false,
        )
        // Success carries no user; the caller only needs to know it worked.
        AuthResult.Success(AuthUser(id = "", identifier = identifier, displayName = "", provider = "password"))
    }

    override suspend fun signOut() {
        val refresh = api.tokens.refreshToken
        if (refresh != null) {
            runCatching { api.post("/auth/signout", JSONObject().put("refresh_token", refresh), auth = false) }
        }
        endSession()
    }

    // ------------------------------------------------------------------------------------ setup

    override fun isSetupPending(): Boolean =
        _currentUser.value?.let { session.isSetupPending(it.id) } == true

    /** Marks setup done locally at once, then tells the server (retrying briefly; the next profile refresh heals it). */
    override fun completeSetup() {
        session.completeSetup()
        scope.launch {
            repeat(3) { attempt ->
                try {
                    api.post("/users/me/setup/complete")
                    return@launch
                } catch (e: ApiException) {
                    if (!e.isNetwork) return@launch
                    delay(2_000L * (attempt + 1))
                }
            }
        }
    }

    override suspend fun discardNewAccount(): Boolean {
        val user = _currentUser.value ?: return false
        if (!session.isSetupPending(user.id)) return false
        return try {
            api.post("/auth/account/discard")
            endSession()
            true
        } catch (_: ApiException) {
            false
        }
    }

    override suspend fun savePersonalSetup(setup: PersonalSetup): Boolean = saveProfileSetup(
        about = setup.about, location = setup.location, latitude = setup.latitude, longitude = setup.longitude,
        places = setup.places, photoUri = setup.photoUri, coverUri = setup.coverUri,
    )

    override suspend fun saveBusinessSetup(setup: BusinessSetup): Boolean = saveProfileSetup(
        about = setup.about, location = setup.location, latitude = setup.latitude, longitude = setup.longitude,
        places = setup.places, photoUri = setup.photoUri, coverUri = setup.coverUri,
    )

    private suspend fun saveProfileSetup(
        about: String,
        location: String,
        latitude: Double?,
        longitude: Double?,
        places: List<PlaceSelection>,
        photoUri: String?,
        coverUri: String?,
    ): Boolean {
        val user = _currentUser.value ?: return false
        return try {
            val main = places.firstOrNull()
            val locationName = main?.name ?: location.trim().ifBlank { null }
            val patch = JSONObject()
                .put("about", about.trim())
                .put("location", locationName ?: JSONObject.NULL)
                .put("latitude", if (locationName != null) (main?.latitude ?: latitude) ?: JSONObject.NULL else JSONObject.NULL)
                .put("longitude", if (locationName != null) (main?.longitude ?: longitude) ?: JSONObject.NULL else JSONObject.NULL)
                .put("places", places.toJson())
            // latitude/longitude must travel together; drop both if either is missing.
            if (patch.isNull("latitude") != patch.isNull("longitude")) {
                patch.put("latitude", JSONObject.NULL).put("longitude", JSONObject.NULL)
            }
            imageField(patch, "photo_media_id", photoUri, ImageUpload.Kind.PROFILE)
            imageField(patch, "cover_media_id", coverUri, ImageUpload.Kind.COVER)
            publish(profileUser(user, api.patch("/users/me", patch).obj()))
            true
        } catch (_: ApiException) {
            false
        }
    }

    /** null clears the image; an http(s) URL is already on the server and is left alone; anything else is uploaded. */
    private suspend fun imageField(patch: JSONObject, field: String, uri: String?, kind: ImageUpload.Kind) {
        when {
            uri == null -> patch.put(field, JSONObject.NULL)
            uri.startsWith("http://") || uri.startsWith("https://") -> Unit
            else -> {
                val bytes = withContext(Dispatchers.IO) { ImageUpload.jpegBytes(context, uri, kind) }
                    ?: throw ApiException(0, "BAD_IMAGE", "Couldn't read that image")
                patch.put(field, api.uploadImage(bytes, kind.apiName).getString("id"))
            }
        }
    }

    override suspend fun savePlaces(places: List<PlaceSelection>): Boolean {
        val user = _currentUser.value ?: return false
        return try {
            publish(profileUser(user, api.put("/users/me/places", places.toJson()).obj()))
            true
        } catch (_: ApiException) {
            false
        }
    }

    // --------------------------------------------------------------------------------- internals

    private suspend fun startSession(auth: JSONObject): AuthResult {
        api.tokens.save(auth.getString("access_token"), auth.getString("refresh_token"))
        val account = auth.getJSONObject("account")
        val profile = try {
            api.get("/users/me").obj()
        } catch (e: ApiException) {
            // Signed up but couldn't load the profile: don't leave a half-signed-in device.
            api.tokens.clear()
            throw e
        }
        val user = accountUser(account, profile)
        session.save(user)
        if (auth.optBoolean("is_new") || profile.optBoolean("setup_pending")) session.markSetupPending(user.id)
        _currentUser.value = user
        return AuthResult.Success(user)
    }

    private suspend fun refreshProfile() {
        val user = _currentUser.value ?: return
        val profile = api.get("/users/me").obj()
        publish(profileUser(user, profile))
        if (profile.optBoolean("setup_pending")) session.markSetupPending(user.id) else session.completeSetup()
    }

    private fun endSession() {
        api.tokens.clear()
        session.clear()
        flow.clearAccountScoped()
        _currentUser.value = null
    }

    private fun publish(updated: AuthUser) {
        session.save(updated)
        _currentUser.value = updated
    }

    private suspend fun guarded(block: suspend () -> AuthResult): AuthResult = try {
        block()
    } catch (e: ApiException) {
        AuthResult.Failure(e.toAuthError(), e.existingType?.let { AccountType.fromName(it) })
    }

    private fun ApiException.toAuthError(): AuthError = when (code) {
        "INVALID_CREDENTIALS" -> AuthError.INVALID_CREDENTIALS
        "IDENTIFIER_TAKEN" -> AuthError.IDENTIFIER_TAKEN
        "USERNAME_TAKEN" -> AuthError.USERNAME_TAKEN
        "EMAIL_TAKEN" -> AuthError.EMAIL_TAKEN
        "ACCOUNT_NOT_FOUND" -> AuthError.ACCOUNT_NOT_FOUND
        "INVALID_CODE" -> AuthError.INVALID_CODE
        "NETWORK" -> AuthError.NETWORK
        else -> AuthError.UNKNOWN
    }

    private fun accountUser(account: JSONObject, profile: JSONObject): AuthUser {
        val base = AuthUser(
            id = account.getString("id"),
            identifier = account.optStringOrNull("identifier") ?: account.optStringOrNull("email").orEmpty(),
            displayName = "",
            provider = account.optString("provider", "password").let { if (it == "local") "password" else it },
            accountType = AccountType.fromName(account.optString("account_type")),
            username = account.optStringOrNull("username"),
            email = account.optStringOrNull("email"),
        )
        return profileUser(base, profile)
    }

    private fun profileUser(base: AuthUser, p: JSONObject): AuthUser = base.copy(
        displayName = p.optString("display_name", base.displayName),
        details = AccountDetails(
            industry = p.optStringOrNull("industry"),
            phone = p.optStringOrNull("phone"),
            registrationId = p.optStringOrNull("registration_id"),
        ),
        birthday = p.optStringOrNull("birthday"),
        about = p.optStringOrNull("about"),
        location = p.optStringOrNull("location"),
        latitude = if (p.isNull("latitude")) null else p.optDouble("latitude"),
        longitude = if (p.isNull("longitude")) null else p.optDouble("longitude"),
        places = p.optJSONArray("places")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { PlaceSelection(it.getString("name"), it.getDouble("lat"), it.getDouble("lng")) }
            }
        }.orEmpty(),
        photoUrl = p.optStringOrNull("photo_url"),
        coverUrl = p.optStringOrNull("cover_url"),
    )

    private fun List<PlaceSelection>.toJson(): JSONArray = JSONArray().also { array ->
        forEach { array.put(JSONObject().put("name", it.name).put("lat", it.latitude).put("lng", it.longitude)) }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    companion object {
        fun create(context: Context): RemoteAuthRepository {
            val app = context.applicationContext
            return RemoteAuthRepository(app, ApiClient.get(app), SessionManager(app), DeviceFlowStore(app))
        }
    }
}
