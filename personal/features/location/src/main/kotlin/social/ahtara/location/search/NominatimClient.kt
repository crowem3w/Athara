package social.ahtara.location.search

import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import social.ahtara.authentication.domain.ServiceArea
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


object NominatimClient {

    



    fun search(query: String, category: PlaceCategory, limit: Int = 5): List<SearchResult> {
        val featureType = when (category) {
            PlaceCategory.COUNTRY -> "country"
            PlaceCategory.PROVINCE -> "state"
            PlaceCategory.CITY -> "city"
        }
        val encoded = URLEncoder.encode(query, "UTF-8")
        val urlStr = "https://nominatim.openstreetmap.org/search?format=json&limit=$limit" +
            "&addressdetails=1&featureType=$featureType&q=$encoded" +
            
            "&countrycodes=${ServiceArea.COUNTRY_CODE}" +
            "&viewbox=${ServiceArea.WEST},${ServiceArea.NORTH},${ServiceArea.EAST},${ServiceArea.SOUTH}&bounded=1"

        val connection = URL(urlStr).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "MapLibreWorldMapDemo/1.0")

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Nominatim HTTP ${connection.responseCode}")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parseResults(JSONArray(body), category)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResults(results: JSONArray, category: PlaceCategory): List<SearchResult> {
        val out = mutableListOf<SearchResult>()
        for (i in 0 until results.length()) {
            val obj = results.getJSONObject(i)
            val lat = obj.optString("lat").toDoubleOrNull() ?: continue
            val lon = obj.optString("lon").toDoubleOrNull() ?: continue
            if (!ServiceArea.contains(lat, lon)) continue 
            val address = obj.optJSONObject("address")
            val displayName = obj.optString("display_name")

            val country = address?.text("country")
            
            val province = address?.firstText("province", "state", "region", "state_district")
            val name = obj.optString("name").takeIf { it.isNotBlank() }
                ?: displayName.substringBefore(",").trim().takeIf { it.isNotBlank() }
                ?: continue

            
            val parents = when (category) {
                PlaceCategory.CITY -> listOfNotNull(province, country)
                PlaceCategory.PROVINCE -> listOfNotNull(country)
                PlaceCategory.COUNTRY -> emptyList()
            }.filter { it != name }.distinct()

            out.add(SearchResult(name, parents.joinToString(", ").ifBlank { null }, LatLng(lat, lon), category))
        }
        return out
    }

    private fun JSONObject.text(key: String): String? = optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.firstText(vararg keys: String): String? = keys.firstNotNullOfOrNull { text(it) }
}