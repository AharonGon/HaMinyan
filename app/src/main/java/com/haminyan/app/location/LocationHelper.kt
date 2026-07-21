package com.haminyan.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Float? = null,
)

class LocationHelper(private val context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** האם אושרה גישת מיקום מדויק (GPS) ולא רק מיקום מקורב */
    fun hasPreciseLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * מחזיר מיקום מדויק ברמת GPS. אוסף עדכונים חיים ב-PRIORITY_HIGH_ACCURACY
     * ומחזיר את הקריאה המדויקת ביותר, בדומה להתנהגות של Waze / Google Maps:
     * ממתין עד לנעילה בדיוק היעד, ואם לא הושג - מחזיר את הטוב ביותר שנאסף.
     */
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(
        desiredAccuracyMeters: Float = 20f,
        maxWaitMs: Long = 12_000L,
    ): GeoPoint? {
        if (!hasPermission()) return null

        var best: Location? = null
        val callbackHolder = arrayOfNulls<LocationCallback>(1)

        withTimeoutOrNull(maxWaitMs) {
            suspendCancellableCoroutine { cont ->
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                    .setMinUpdateIntervalMillis(250L)
                    .setWaitForAccurateLocation(true)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        val location = result.lastLocation ?: return
                        if (isMoreAccurate(location, best)) best = location
                        // נעילה מספקת - מסיימים מוקדם
                        if (location.hasAccuracy() && location.accuracy <= desiredAccuracyMeters && cont.isActive) {
                            cont.resume(Unit)
                        }
                    }
                }
                callbackHolder[0] = callback
                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                cont.invokeOnCancellation { client.removeLocationUpdates(callback) }
            }
        }
        callbackHolder[0]?.let { client.removeLocationUpdates(it) }

        // גיבוי 1: בקשת fix בודד ברמת דיוק גבוהה
        if (best == null) {
            best = runCatching {
                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
            }.getOrNull()
        }

        // גיבוי 2: מיקום אחרון ידוע - רק אם הוא טרי (לא מיקום ישן ממקום אחר)
        if (best == null) {
            best = runCatching { client.lastLocation.await() }.getOrNull()?.takeIf { isFresh(it) }
        }

        return best?.let {
            GeoPoint(
                lat = it.latitude,
                lng = it.longitude,
                accuracyMeters = if (it.hasAccuracy()) it.accuracy else null,
            )
        }
    }

    private fun isMoreAccurate(candidate: Location, current: Location?): Boolean {
        if (current == null) return true
        if (!candidate.hasAccuracy()) return false
        if (!current.hasAccuracy()) return true
        return candidate.accuracy < current.accuracy
    }

    /** מיקום נחשב טרי אם התקבל בשתי הדקות האחרונות */
    private fun isFresh(location: Location): Boolean {
        val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        return ageMs in 0..120_000L
    }
}
