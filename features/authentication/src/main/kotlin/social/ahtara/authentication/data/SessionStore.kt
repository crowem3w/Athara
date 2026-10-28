package social.ahtara.authentication.data

import android.content.Context
import androidx.core.content.edit
import social.ahtara.authentication.domain.AccountDetails
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.AuthUser
import social.ahtara.authentication.domain.placesFromJson
import social.ahtara.authentication.domain.toJsonString


internal class SessionStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("authentication_session", Context.MODE_PRIVATE)

    fun read(): AuthUser? {
        val id = prefs.getString(KEY_ID, null) ?: return null
        return AuthUser(
            id = id,
            identifier = prefs.getString(KEY_IDENTIFIER, "").orEmpty(),
            displayName = prefs.getString(KEY_NAME, "").orEmpty(),
            provider = prefs.getString(KEY_PROVIDER, "password").orEmpty(),
            accountType = AccountType.fromName(prefs.getString(KEY_ACCOUNT_TYPE, null)),
            details = AccountDetails(
                industry = prefs.getString(KEY_INDUSTRY, null),
                phone = prefs.getString(KEY_PHONE, null),
                registrationId = prefs.getString(KEY_REGISTRATION_ID, null),
            ),
            username = prefs.getString(KEY_USERNAME, null),
            email = prefs.getString(KEY_EMAIL, null),
            birthday = prefs.getString(KEY_BIRTHDAY, null),
            about = prefs.getString(KEY_ABOUT, null),
            location = prefs.getString(KEY_LOCATION, null),
            latitude = prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull(),
            longitude = prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull(),
            places = placesFromJson(prefs.getString(KEY_PLACES, null)),
            photoPath = prefs.getString(KEY_PHOTO_PATH, null),
            coverPath = prefs.getString(KEY_COVER_PATH, null),
        )
    }

    fun save(user: AuthUser) = prefs.edit {
        putString(KEY_ID, user.id)
        putString(KEY_IDENTIFIER, user.identifier)
        putString(KEY_NAME, user.displayName)
        putString(KEY_PROVIDER, user.provider)
        putString(KEY_ACCOUNT_TYPE, user.accountType.name)
        putString(KEY_INDUSTRY, user.details.industry)
        putString(KEY_PHONE, user.details.phone)
        putString(KEY_REGISTRATION_ID, user.details.registrationId)
        putString(KEY_USERNAME, user.username)
        putString(KEY_EMAIL, user.email)
        putString(KEY_BIRTHDAY, user.birthday)
        putString(KEY_ABOUT, user.about)
        putString(KEY_LOCATION, user.location)
        putString(KEY_LATITUDE, user.latitude?.toString())
        putString(KEY_LONGITUDE, user.longitude?.toString())
        putString(KEY_PLACES, user.places.takeIf { it.isNotEmpty() }?.toJsonString())
        putString(KEY_PHOTO_PATH, user.photoPath)
        putString(KEY_COVER_PATH, user.coverPath)
    }

    
    fun markSetupPending(userId: String) = prefs.edit { putString(KEY_SETUP_PENDING, userId) }

    fun isSetupPending(userId: String): Boolean = prefs.getString(KEY_SETUP_PENDING, null) == userId

    fun completeSetup() = prefs.edit { remove(KEY_SETUP_PENDING) }

    fun clear() = prefs.edit { clear() }

    private companion object {
        const val KEY_ID = "id"
        const val KEY_SETUP_PENDING = "setup_pending_id"
        const val KEY_IDENTIFIER = "identifier"
        const val KEY_NAME = "name"
        const val KEY_PROVIDER = "provider"
        const val KEY_ACCOUNT_TYPE = "account_type"
        const val KEY_INDUSTRY = "industry"
        const val KEY_PHONE = "phone"
        const val KEY_REGISTRATION_ID = "registration_id"
        const val KEY_USERNAME = "username"
        const val KEY_EMAIL = "email"
        const val KEY_BIRTHDAY = "birthday"
        const val KEY_ABOUT = "about"
        const val KEY_LOCATION = "location"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_PLACES = "places"
        const val KEY_PHOTO_PATH = "photo_path"
        const val KEY_COVER_PATH = "cover_path"
    }
}