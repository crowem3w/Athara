package social.ahtara.authentication.domain

import org.json.JSONArray
import org.json.JSONObject


data class PlaceSelection(
    val name: String,
    val latitude: Double,
    val longitude: Double,
)


fun List<PlaceSelection>.toJsonString(): String =
    JSONArray().also { array ->
        forEach { array.put(JSONObject().put("name", it.name).put("lat", it.latitude).put("lng", it.longitude)) }
    }.toString()


fun placesFromJson(json: String?): List<PlaceSelection> {
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