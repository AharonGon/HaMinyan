package com.haminyan.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haminyan.app.MinyanApp
import com.haminyan.app.data.MinyanRepository
import com.haminyan.app.data.PrefsStore
import com.haminyan.app.data.model.FavoriteMosad
import com.haminyan.app.data.model.MinyanItem
import com.haminyan.app.util.DayUtils
import com.haminyan.app.util.ErrorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val errorDetails: String? = null,
    val schedule: List<MinyanItem> = emptyList(),
    val selectedDay: Char = DayUtils.todayLetter(),
) {
    /** לוח היום הנבחר, ממויין לפי שעה ומקובץ לפי סוג תפילה */
    val daySchedule: Map<String, List<MinyanItem>>
        get() = schedule
            .filter { DayUtils.isActiveOnDay(it.days, selectedDay) }
            .sortedBy { DayUtils.parseTime(it.time) }
            .groupBy { it.type?.trim().takeUnless { t -> t.isNullOrEmpty() } ?: "אחר" }
}

class DetailViewModel(
    private val mosadId: String,
    private val mosadName: String,
    private val repository: MinyanRepository,
    private val prefs: PrefsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    val isFavorite: StateFlow<Boolean> = prefs.favorites
        .map { list -> list.any { it.id == mosadId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, errorDetails = null) }
            runCatching { repository.mosadSchedule(mosadId) }
                .onSuccess { list ->
                    _state.update { it.copy(loading = false, schedule = list) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = ErrorInfo.friendly(e),
                            errorDetails = ErrorInfo.technical(e, "GetMosadMinyan id=$mosadId"),
                        )
                    }
                }
        }
    }

    fun selectDay(day: Char) {
        _state.update { it.copy(selectedDay = day) }
    }

    fun toggleFavorite() {
        viewModelScope.launch { prefs.toggleFavorite(FavoriteMosad(mosadId, mosadName)) }
    }

    companion object {
        fun factory(app: MinyanApp, mosadId: String, mosadName: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DetailViewModel(mosadId, mosadName, app.repository, app.prefs) as T
            }
    }
}
