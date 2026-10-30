package social.ahtara.authentication.data

import android.content.Context
import androidx.core.content.edit
import social.ahtara.authentication.domain.AccountType
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.ServiceArea
import social.ahtara.authentication.domain.placesFromJson
import social.ahtara.authentication.domain.toJsonString








internal class DeviceFlowStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun selectedType(): AccountType? =
        prefs.getString(KEY_ACCOUNT_TYPE, null)?.let { name -> AccountType.available.firstOrNull { it.name == name } }

    fun selectType(type: AccountType) = prefs.edit { putString(KEY_ACCOUNT_TYPE, type.name) }

    fun places(): List<PlaceSelection> =
        placesFromJson(prefs.getString(KEY_PLACES, null)).filter { ServiceArea.contains(it) }

    fun savePlaces(places: List<PlaceSelection>) = prefs.edit { putString(KEY_PLACES, places.toJsonString()) }

    fun isLocationStepDone(): Boolean = prefs.getBoolean(KEY_LOCATION_DONE, false)

    fun markLocationStepDone() = prefs.edit { putBoolean(KEY_LOCATION_DONE, true) }

    fun address(): String = prefs.getString(KEY_ADDRESS, "").orEmpty()

    fun saveAddress(address: String) = prefs.edit { putString(KEY_ADDRESS, address.trim()) }

    
    fun clearAccountScoped() = prefs.edit { remove(KEY_ADDRESS) }

    private companion object {
        
        const val PREFS = "authentication_flow"
        const val KEY_ACCOUNT_TYPE = "selected_account_type"
        const val KEY_PLACES = "device_places"
        const val KEY_LOCATION_DONE = "location_step_done"
        const val KEY_ADDRESS = "device_address"
    }
}