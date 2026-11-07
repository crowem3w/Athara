package soc.athara.app.modules.personal.features.travel.model

import org.json.JSONArray
import org.json.JSONObject


internal data class PlaceSelection(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)


internal fun List<PlaceSelection>.toJsonString(): String =
    JSONArray().also { array ->
        forEach { array.put(JSONObject().put("name", it.name).put("lat", it.latitude).put("lng", it.longitude)) }
    }.toString()

