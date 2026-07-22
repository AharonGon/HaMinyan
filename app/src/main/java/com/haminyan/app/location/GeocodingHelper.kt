package com.haminyan.app.location

import android.content.Context
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object GeocodingHelper {

    /** שם היישוב לפי קואורדינטות, למשל "מודיעין עילית" */
    suspend fun localityName(context: Context, lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            runCatching {
                val geocoder = Geocoder(context, Locale("he", "IL"))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lng, 1) { addresses ->
                            cont.resume(pickLocality(addresses.firstOrNull()))
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    pickLocality(geocoder.getFromLocation(lat, lng, 1)?.firstOrNull())
                }
            }.getOrNull()
        }

    private fun pickLocality(address: android.location.Address?): String? {
        if (address == null) return null
        return address.locality
            ?: address.subLocality
            ?: address.subAdminArea
            ?: address.adminArea
    }
}
