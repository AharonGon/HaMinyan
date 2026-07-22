package com.haminyan.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haminyan.app.MinyanApp
import com.haminyan.app.location.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class StartupViewModel(
    private val locationHelper: LocationHelper,
) : ViewModel() {

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    init {
        viewModelScope.launch {
            // מינימום קצר כדי שהתמונה תיראה גם כשהמיקום מגיע מיד
            delay(1_200)
            if (locationHelper.hasPermission()) {
                withTimeoutOrNull(12_000) {
                    locationHelper.currentLocation()
                }
            }
            _showSplash.value = false
        }
    }

    companion object {
        fun factory(app: MinyanApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                StartupViewModel(app.locationHelper) as T
        }
    }
}
