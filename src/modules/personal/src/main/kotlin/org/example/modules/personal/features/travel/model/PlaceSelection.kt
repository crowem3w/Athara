package org.example.modules.personal.features.travel.model

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


internal fun placesFromJson(json: String?): List<PlaceSelection> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            if (!o.has("lat") || !o.has("lng")) return@mapNotNull null
            PlaceSelection(name, o.getDouble("lat"), o.getDouble("lng"))
        }
    } catch (e: org.json.JSONException) {
        emptyList()
    }
}