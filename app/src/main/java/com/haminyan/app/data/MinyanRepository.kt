package com.haminyan.app.data

import com.haminyan.app.data.model.MinyanItem
import com.haminyan.app.data.model.MosadResult
import com.haminyan.app.data.model.NearbyMinyan

class MinyanRepository(private val api: NedarimApi) {

    suspend fun search(query: String): List<MosadResult> =
        api.searchMosad(query).clientBox.orEmpty()
            .filter { it.mosadId.isNotBlank() && it.mosadName.isNotBlank() }

    suspend fun mosadSchedule(mosadId: String): List<MinyanItem> =
        api.getMosadMinyan(mosadId).data.orEmpty()
            .filter { !it.time.isNullOrBlank() }

    suspend fun nearby(lat: Double, lng: Double, radiusKm: Int): List<NearbyMinyan> =
        api.getNearestMinyan(lat, lng, radiusKm).data.orEmpty()
            .filter { !it.time.isNullOrBlank() }
}
