package social.ahtara.app

import android.content.Context
import social.ahtara.authentication.Authentication
import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.ServiceArea

/** Places shown on Home for signed-out (guest) visitors: Guimaras only. */
object GuestPlaces {
    val default: List<PlaceSelection> = listOf(ServiceArea.default)

    /** Places picked on the first-launch Map/Location step; falls back to the whole province. */
    fun forDevice(context: Context): List<PlaceSelection> =
        Authentication.devicePlaces(context).ifEmpty { default }
}
