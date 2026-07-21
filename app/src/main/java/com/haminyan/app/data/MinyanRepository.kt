package com.haminyan.app.data

import android.os.SystemClock
import com.haminyan.app.data.model.MinyanItem
import com.haminyan.app.data.model.MosadResult
import com.haminyan.app.data.model.NearbyMinyan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MinyanRepository(
    private val api: NedarimApi,
    private val routing: RoutingApi? = null,
) {

    // חיפוש ולוח זמנים כמעט אינם משתנים - ניתן לשמור במטמון לזמן ארוך יחסית
    private val searchCache = TtlCache<String, List<MosadResult>>(ttlMillis = 5 * 60_000L)
    private val scheduleCache = TtlCache<String, List<MinyanItem>>(ttlMillis = 10 * 60_000L)

    // תוצאות "בקרבתי" - מטמון קצר, ורק אם המשתמש כמעט לא זז (לשמירת דיוק)
    private data class NearbyEntry(
        val lat: Double,
        val lng: Double,
        val radiusKm: Int,
        val at: Long,
        val data: List<NearbyMinyan>,
    )

    @Volatile
    private var nearbyEntry: NearbyEntry? = null

    suspend fun search(query: String): List<MosadResult> {
        val key = query.trim()
        searchCache.get(key)?.let { return it }
        val result = api.searchMosad(key).clientBox.orEmpty()
            .filter { it.mosadId.isNotBlank() && it.mosadName.isNotBlank() }
        searchCache.put(key, result)
        return result
    }

    suspend fun mosadSchedule(mosadId: String): List<MinyanItem> {
        scheduleCache.get(mosadId)?.let { return it }
        val result = api.getMosadMinyan(mosadId).data.orEmpty()
            .filter { !it.time.isNullOrBlank() }
        scheduleCache.put(mosadId, result)
        return result
    }

    suspend fun nearby(lat: Double, lng: Double, radiusKm: Int): List<NearbyMinyan> {
        nearbyEntry?.let { cache ->
            val fresh = SystemClock.elapsedRealtime() - cache.at < NEARBY_TTL_MS
            val samePlace = haversineMeters(lat, lng, cache.lat, cache.lng) <= NEARBY_MAX_MOVE_M
            if (cache.radiusKm == radiusKm && fresh && samePlace) return cache.data
        }

        val base = api.getNearestMinyan(lat, lng, radiusKm).data.orEmpty()
            .filter { !it.time.isNullOrBlank() }
        // כשל בשירות הניתוב לא יפיל את המסך - נחזור למרחק האווירי
        val result = if (routing == null || base.isEmpty()) base
        else runCatching { enrichWithWalking(lat, lng, base) }.getOrDefault(base)

        nearbyEntry = NearbyEntry(lat, lng, radiusKm, SystemClock.elapsedRealtime(), result)
        return result
    }

    /**
     * מחשב מרחק/זמן הליכה אמיתי לכל בית כנסת בבקשת Matrix אחת.
     * נקודות זהות (אותו בית כנסת עם כמה מניינים) נשלחות פעם אחת בלבד
     * כדי לצמצם את גודל הבקשה ולשמור על מכסת ה-API.
     */
    private suspend fun enrichWithWalking(
        userLat: Double,
        userLng: Double,
        items: List<NearbyMinyan>,
    ): List<NearbyMinyan> {
        val uniquePoints: List<Pair<Double, Double>> = items
            .filter { it.hasCoords }
            .map { it.lat!! to it.lng!! }
            .distinct()
        if (uniquePoints.isEmpty()) return items

        // אינדקס 0 = מיקום המשתמש; שאר האינדקסים = בתי הכנסת (סדר [lng, lat] ל-ORS)
        val locations = buildList {
            add(listOf(userLng, userLat))
            uniquePoints.forEach { (pLat, pLng) -> add(listOf(pLng, pLat)) }
        }
        val response = routing!!.walkingMatrix(
            MatrixRequest(
                locations = locations,
                sources = listOf(0),
                destinations = uniquePoints.indices.map { it + 1 },
            )
        )

        val distances = response.distances?.firstOrNull()
        val durations = response.durations?.firstOrNull()

        val metersByPoint = HashMap<Pair<Double, Double>, Double?>()
        val secondsByPoint = HashMap<Pair<Double, Double>, Double?>()
        uniquePoints.forEachIndexed { index, point ->
            metersByPoint[point] = distances?.getOrNull(index)
            secondsByPoint[point] = durations?.getOrNull(index)
        }

        return items.map { minyan ->
            if (!minyan.hasCoords) return@map minyan
            val point = minyan.lat!! to minyan.lng!!
            minyan.copy(
                walkMeters = metersByPoint[point],
                walkSeconds = secondsByPoint[point],
            )
        }
    }

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        private const val NEARBY_TTL_MS = 45_000L
        private const val NEARBY_MAX_MOVE_M = 40.0
    }
}
