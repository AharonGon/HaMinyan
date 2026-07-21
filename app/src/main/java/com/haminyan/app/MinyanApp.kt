package com.haminyan.app

import android.app.Application
import com.haminyan.app.data.MinyanRepository
import com.haminyan.app.data.NedarimApi
import com.haminyan.app.data.PrefsStore
import com.haminyan.app.data.RoutingApi
import com.haminyan.app.location.LocationHelper
import com.haminyan.app.update.UpdateChecker

class MinyanApp : Application() {

    private val routingApi: RoutingApi? by lazy {
        BuildConfig.ORS_API_KEY.takeIf { it.isNotBlank() }?.let { RoutingApi.create(it) }
    }

    val repository: MinyanRepository by lazy {
        MinyanRepository(NedarimApi.create(), routingApi)
    }
    val prefs: PrefsStore by lazy { PrefsStore(this) }
    val locationHelper: LocationHelper by lazy { LocationHelper(this) }
    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(BuildConfig.GITHUB_REPOSITORY)
    }
}
