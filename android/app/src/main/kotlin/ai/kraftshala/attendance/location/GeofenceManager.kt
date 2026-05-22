package ai.kraftshala.attendance.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Returns whether the user is inside the campus geofence based on GPS.
 * Campus is defined as a polygon in the backend (geofence_geojson on campuses table).
 */
class GeofenceManager(private val context: Context) {

    private val client: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun isWithinPolygon(polygonLatLngs: List<Pair<Double, Double>>): Boolean {
        val (lat, lng) = currentLatLng() ?: return false
        return pointInPolygon(lat, lng, polygonLatLngs)
    }

    @SuppressLint("MissingPermission")
    suspend fun currentLatLng(): Pair<Double, Double>? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) cont.resume(loc.latitude to loc.longitude)
                else cont.resume(null)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    /** Ray casting algorithm for point-in-polygon. */
    private fun pointInPolygon(lat: Double, lng: Double, poly: List<Pair<Double, Double>>): Boolean {
        if (poly.size < 3) return false
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val (xi, yi) = poly[i]
            val (xj, yj) = poly[j]
            if ((yi > lng) != (yj > lng) &&
                (lat < (xj - xi) * (lng - yi) / (yj - yi) + xi)
            ) inside = !inside
            j = i
        }
        return inside
    }
}
