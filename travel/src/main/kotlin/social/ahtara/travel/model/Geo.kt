package social.ahtara.travel.model

import kotlin.math.cos
import kotlin.math.sqrt


fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val x = Math.toRadians(lon2 - lon1) * cos(Math.toRadians((lat1 + lat2) / 2))
    val y = Math.toRadians(lat2 - lat1)
    return sqrt(x * x + y * y) * 6_371_000
}