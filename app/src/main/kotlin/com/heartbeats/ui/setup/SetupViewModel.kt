package com.heartbeats.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeats.data.repository.SettingsRepository
import com.heartbeats.domain.model.HapticMode
import com.heartbeats.domain.model.WarmupDuration
import com.heartbeats.domain.model.ZonePreset
import com.heartbeats.domain.model.ZoneRange
import com.heartbeats.domain.usecase.ComputeZoneRangeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val age: Int = DEFAULT_AGE,
    val preset: ZonePreset = ZonePreset.DEFAULT,
    val warmup: WarmupDuration = WarmupDuration.DEFAULT,
    val ranges: Map<ZonePreset, ZoneRange?> = emptyMap(),
) {
    companion object {
        const val MIN_AGE = 5
        const val MAX_AGE = 100
        const val DEFAULT_AGE = 30
    }
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val computeZoneRange: ComputeZoneRangeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState(ranges = rangesFor(SetupUiState.DEFAULT_AGE)))
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun changeAge(delta: Int) {
        _uiState.update {
            val age = (it.age + delta).coerceIn(SetupUiState.MIN_AGE, SetupUiState.MAX_AGE)
            it.copy(age = age, ranges = rangesFor(age))
        }
    }

    fun selectPreset(preset: ZonePreset) {
        _uiState.update { it.copy(preset = preset) }
    }

    fun selectWarmup(warmup: WarmupDuration) {
        _uiState.update { it.copy(warmup = warmup) }
    }

    fun finish(onDone: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            settingsRepository.completeSetup(age = state.age, preset = state.preset, warmup = state.warmup)
            settingsRepository.setHapticMode(HapticMode.DEFAULT)
            onDone()
        }
    }

    private fun rangesFor(age: Int): Map<ZonePreset, ZoneRange?> =
        ZonePreset.entries.associateWith { computeZoneRange(age, it) }
}
