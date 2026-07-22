package com.haminyan.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haminyan.app.MinyanApp
import com.haminyan.app.data.HalachicTime
import com.haminyan.app.data.HebcalRepository
import com.haminyan.app.data.JewishDayInfo
import com.haminyan.app.data.ZmanimGroup
import com.haminyan.app.location.GeocodingHelper
import com.haminyan.app.location.LocationHelper
import com.haminyan.app.util.ErrorInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ZmanimUiState(
    val loading: Boolean = false,
    val permissionDenied: Boolean = false,
    val coarseOnly: Boolean = false,
    val locationAccuracy: Float? = null,
    val locationLabel: String? = null,
    val error: String? = null,
    val errorDetails: String? = null,
    val zmanim: List<HalachicTime> = emptyList(),
    val dayInfo: JewishDayInfo? = null,
    val nextZmanKey: String? = null,
    val hasLoadedOnce: Boolean = false,
) {
    val groupedZmanim: Map<ZmanimGroup, List<HalachicTime>>
        get() = zmanim.groupBy { it.group }
}

class ZmanimViewModel(
    private val hebcal: HebcalRepository,
    private val locationHelper: LocationHelper,
    private val app: MinyanApp,
) : ViewModel() {

    private val _state = MutableStateFlow(ZmanimUiState())
    val state: StateFlow<ZmanimUiState> = _state.asStateFlow()

    init {
        if (locationHelper.hasPermission()) refresh()
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionDenied = !granted) }
        if (granted) refresh()
    }

    fun refresh() {
        if (!locationHelper.hasPermission()) {
            _state.update { it.copy(permissionDenied = true) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, errorDetails = null) }
            try {
                val point = locationHelper.currentLocation()
                    ?: throw IllegalStateException("לא התקבל מיקום מהמכשיר")

                val (zmanim, dayInfo, locality) = coroutineScope {
                    val zmanimDeferred = async { hebcal.zmanim(point.lat, point.lng) }
                    val dayInfoDeferred = async { hebcal.dayInfo() }
                    val localityDeferred = async {
                        GeocodingHelper.localityName(app, point.lat, point.lng)
                    }
                    Triple(zmanimDeferred.await(), dayInfoDeferred.await(), localityDeferred.await())
                }

                _state.update {
                    it.copy(
                        loading = false,
                        permissionDenied = false,
                        coarseOnly = !locationHelper.hasPreciseLocation(),
                        locationAccuracy = point.accuracyMeters,
                        locationLabel = locality ?: "מיקום לא ידוע",
                        zmanim = zmanim,
                        dayInfo = dayInfo,
                        nextZmanKey = HebcalRepository.nextZman(zmanim),
                        hasLoadedOnce = true,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = ErrorInfo.friendly(e),
                        errorDetails = ErrorInfo.technical(e, "ZmanimViewModel.refresh"),
                        hasLoadedOnce = true,
                    )
                }
            }
        }
    }

    companion object {
        fun factory(app: MinyanApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ZmanimViewModel(app.hebcalRepository, app.locationHelper, app) as T
            }
        }
    }
}
