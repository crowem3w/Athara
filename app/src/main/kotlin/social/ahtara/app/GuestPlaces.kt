package social.ahtara.app

import android.content.Context
import social.ahtara.authentication.Authentication
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.ServiceArea


object GuestPlaces {
    val default: List<PlaceSelection> = listOf(ServiceArea.default)

    
    fun forDevice(context: Context): List<PlaceSelection> =
        Authentication.devicePlaces(context).ifEmpty { default }
}