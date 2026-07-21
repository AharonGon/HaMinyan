package com.haminyan.app.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.haminyan.app.MinyanApp
import com.haminyan.app.update.UpdateChecker
import com.haminyan.app.update.UpdateInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUiState(
    val checking: Boolean = false,
    val update: UpdateInfo? = null,
    val checkedAndCurrent: Boolean = false,
    val error: String? = null,
)

class UpdateViewModel(
    private val checker: UpdateChecker,
) : ViewModel() {
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var startupCheckPerformed = false

    fun checkOnStartup() {
        if (startupCheckPerformed) return
        startupCheckPerformed = true
        check(showCurrentResult = false)
    }

    fun checkManually() = check(showCurrentResult = true)

    fun dismissResult() {
        _state.update { it.copy(update = null, checkedAndCurrent = false, error = null) }
    }

    private fun check(showCurrentResult: Boolean) {
        if (_state.value.checking) return
        viewModelScope.launch {
            _state.update {
                it.copy(checking = true, checkedAndCurrent = false, error = null)
            }
            runCatching { checker.check() }
                .onSuccess { update ->
                    _state.update {
                        it.copy(
                            checking = false,
                            update = update,
                            checkedAndCurrent = showCurrentResult && update == null,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            checking = false,
                            error = if (showCurrentResult) {
                                "לא ניתן לבדוק עדכונים כרגע. בדקו את החיבור לרשת."
                            } else null,
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(app: MinyanApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UpdateViewModel(app.updateChecker) as T
        }
    }
}
