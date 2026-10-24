package social.ahtara.app

import social.ahtara.authentication.domain.PlaceSelection
import social.ahtara.authentication.domain.ServiceArea

/** Places shown on Home for signed-out (guest) visitors: Guimaras only. */
object GuestPlaces {
    val default: List<PlaceSelection> = listOf(ServiceArea.default)
}
