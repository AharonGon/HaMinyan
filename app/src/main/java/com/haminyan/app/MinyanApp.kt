package com.haminyan.app

import android.app.Application
import com.haminyan.app.data.MinyanRepository
import com.haminyan.app.data.NedarimApi
import com.haminyan.app.data.PrefsStore
import com.haminyan.app.location.LocationHelper
import com.haminyan.app.update.UpdateChecker

class MinyanApp : Application() {

    val repository: MinyanRepository by lazy { MinyanRepository(NedarimApi.create()) }
    val prefs: PrefsStore by lazy { PrefsStore(this) }
    val locationHelper: LocationHelper by lazy { LocationHelper(this) }
    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(BuildConfig.GITHUB_REPOSITORY)
    }
}
