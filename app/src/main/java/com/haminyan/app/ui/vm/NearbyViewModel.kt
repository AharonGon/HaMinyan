package com.haminyan.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haminyan.app.MinyanApp
import com.haminyan.app.data.MinyanRepository
import com.haminyan.app.data.PrefsStore
import com.haminyan.app.data.model.NearbyMinyan
import com.haminyan.app.location.GeoPoint
import com.haminyan.app.location.LocationHelper
import com.haminyan.app.util.DayUtils
import com.haminyan.app.util.ErrorInfo
import com.haminyan.app.util.RadiusFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

enum class NearbySortMode { DISTANCE, TIME }

data class NearbyUiState(
    val loading: Boolean = false,
    val permissionDenied: Boolean = false,
    val coarseOnly: Boolean = false,
    val locationAccuracy: Float? = null,
    val error: String? = null,
    val errorDetails: String? = null,
    val minyanim: List<NearbyMinyan> = emptyList(),
    val typeFilter: String? = null,
    val sortMode: NearbySortMode = NearbySortMode.DISTANCE,
    val radiusMeters: Int = 2_000,
    val lastUpdated: String? = null,
    val hasLoadedOnce: Boolean = false,
) {
    val availableTypes: List<String>
        get() = minyanim.mapNotNull { it.type?.trim() }.filter { it.isNotEmpty() }.distinct()

    val filtered: List<NearbyMinyan>
        get() {
            val base = if (typeFilter == null) {
                minyanim
            } else {
                minyanim.filter { it.type?.trim() == typeFilter }
            }
            return when (sortMode) {
                NearbySortMode.DISTANCE -> base.sortedBy { it.effectiveMeters }
                NearbySortMode.TIME -> base.sortedWith(
                    compareBy(
                        { DayUtils.minutesUntil(it.time) == null },
                        { DayUtils.parseTime(it.time) ?: LocalTime.MAX },
                    )
                )
            }
        }

    /** האם קיבלנו מרחקי הליכה אמיתיים (ORS) עבור לפחות תוצאה אחת */
    val hasWalkingData: Boolean
        get() = minyanim.any { it.walkMeters != null }
}

class NearbyViewModel(
    private val repository: MinyanRepository,
    private val locationHelper: LocationHelper,
    private val prefs: PrefsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(NearbyUiState())
    val state: StateFlow<NearbyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val radius = prefs.radiusMeters.first()
            _state.update { it.copy(radiusMeters = radius) }
            if (locationHelper.hasPermission()) refresh()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _state.update { it.copy(permissionDenied = false) }
            refresh()
        } else {
            _state.update { it.copy(permissionDenied = true, loading = false) }
        }
    }

    fun setRadius(meters: Int) {
        _state.update { it.copy(radiusMeters = meters) }
        viewModelScope.launch { prefs.setRadiusMeters(meters) }
        refresh()
    }

    fun setTypeFilter(type: String?) {
        _state.update { it.copy(typeFilter = type) }
    }

    fun setSortMode(mode: NearbySortMode) {
        _state.update { it.copy(sortMode = mode) }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true, error = null, errorDetails = null, coarseOnly = locationHelper.hasPermission() && !locationHelper.hasPreciseLocation())
            }
            val location: GeoPoint? = locationHelper.currentLocation()
            if (location == null) {
                _state.update {
                    it.copy(
                        loading = false,
                        permissionDenied = !locationHelper.hasPermission(),
                        error = if (locationHelper.hasPermission()) "לא הצלחנו לאתר את המיקום הנוכחי. ודאו ש-GPS מופעל ונסו שוב." else null,
                    )
                }
                return@launch
            }
            val radiusMeters = _state.value.radiusMeters
            val apiRadiusKm = RadiusFormat.apiRadiusKm(radiusMeters)
            runCatching {
                repository.nearby(location.lat, location.lng, apiRadiusKm)
                    .filter { it.effectiveMeters <= radiusMeters }
            }.onSuccess { list ->
                _state.update {
                    it.copy(
                        loading = false,
                        minyanim = list,
                        locationAccuracy = location.accuracyMeters,
                        hasLoadedOnce = true,
                        lastUpdated = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = ErrorInfo.friendly(e),
                        errorDetails = ErrorInfo.technical(e, "GetNearestMinyan radius=${radiusMeters}m"),
                        hasLoadedOnce = true,
                    )
                }
            }
        }
    }

    companion object {
        fun factory(app: MinyanApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                NearbyViewModel(app.repository, app.locationHelper, app.prefs) as T
        }
    }
}
