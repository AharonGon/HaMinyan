package com.haminyan.app.data

import com.haminyan.app.data.model.MinyanItem
import com.haminyan.app.data.model.MosadResult
import com.haminyan.app.data.model.NearbyMinyan

class MinyanRepository(
    private val api: NedarimApi,
    private val routing: RoutingApi? = null,
) {

    suspend fun search(query: String): List<MosadResult> =
        api.searchMosad(query).clientBox.orEmpty()
            .filter { it.mosadId.isNotBlank() && it.mosadName.isNotBlank() }

    suspend fun mosadSchedule(mosadId: String): List<MinyanItem> =
        api.getMosadMinyan(mosadId).data.orEmpty()
            .filter { !it.time.isNullOrBlank() }

    suspend fun nearby(lat: Double, lng: Double, radiusKm: Int): List<NearbyMinyan> {
        val base = api.getNearestMinyan(lat, lng, radiusKm).data.orEmpty()
            .filter { !it.time.isNullOrBlank() }
        if (routing == null || base.isEmpty()) return base
        // כשל בשירות הניתוב לא יפיל את המסך - נחזור למרחק האווירי
        return runCatching { enrichWithWalking(lat, lng, base) }.getOrDefault(base)
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
}
