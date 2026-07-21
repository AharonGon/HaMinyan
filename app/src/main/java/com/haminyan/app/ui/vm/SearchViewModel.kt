package com.haminyan.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haminyan.app.MinyanApp
import com.haminyan.app.data.MinyanRepository
import com.haminyan.app.data.PrefsStore
import com.haminyan.app.data.model.MosadResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<MosadResult> = emptyList(),
    val error: String? = null,
    val searched: Boolean = false,
)

class SearchViewModel(
    private val repository: MinyanRepository,
    private val prefs: PrefsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val recentSearches: StateFlow<List<String>> = prefs.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update { it.copy(results = emptyList(), loading = false, searched = false, error = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400) // debounce
            performSearch(query.trim())
        }
    }

    fun searchNow(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(query.trim()) }
    }

    fun onResultChosen() {
        val q = _state.value.query.trim()
        if (q.isNotEmpty()) viewModelScope.launch { prefs.addRecentSearch(q) }
    }

    fun clearRecents() {
        viewModelScope.launch { prefs.clearRecentSearches() }
    }

    private suspend fun performSearch(query: String) {
        if (query.isEmpty()) return
        _state.update { it.copy(loading = true, error = null) }
        runCatching { repository.search(query) }
            .onSuccess { list ->
                _state.update { it.copy(loading = false, results = list, searched = true) }
            }
            .onFailure {
                _state.update {
                    it.copy(loading = false, error = "שגיאה בחיפוש. בדקו את החיבור לרשת.", searched = true)
                }
            }
    }

    companion object {
        fun factory(app: MinyanApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SearchViewModel(app.repository, app.prefs) as T
        }
    }
}
