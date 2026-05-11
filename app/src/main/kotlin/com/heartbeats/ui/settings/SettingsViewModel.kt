package com.heartbeats.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeats.data.repository.SettingsRepository
import com.heartbeats.domain.model.HapticMode
import com.heartbeats.domain.model.WarmupDuration
import com.heartbeats.domain.model.ZonePreset
import com.heartbeats.domain.model.ZoneRange
import com.heartbeats.domain.usecase.ComputeZoneRangeUseCase
import com.heartbeats.media.MediaControllerManager
import com.heartbeats.ui.setup.SetupUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val age: Int = SetupUiState.DEFAULT_AGE,
    val preset: ZonePreset = ZonePreset.DEFAULT,
    val warmup: WarmupDuration = WarmupDuration.DEFAULT,
    val hapticMode: HapticMode = HapticMode.DEFAULT,
    val ranges: Map<ZonePreset, ZoneRange?> = emptyMap(),
    val notificationAccessGranted: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val computeZoneRange: ComputeZoneRangeUseCase,
    private val mediaController: MediaControllerManager,
) : ViewModel() {

    private val notificationAccess = MutableStateFlow(mediaController.hasNotificationAccess())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.age,
        settingsRepository.zonePreset,
        settingsRepository.warmupDuration,
        settingsRepository.hapticMode,
        notificationAccess,
    ) { age, preset, warmup, haptics, hasAccess ->
        val effectiveAge = age ?: SetupUiState.DEFAULT_AGE
        SettingsUiState(
            age = effectiveAge,
            preset = preset,
            warmup = warmup,
            hapticMode = haptics,
            ranges = ZonePreset.entries.associateWith { computeZoneRange(effectiveAge, it) },
            notificationAccessGranted = hasAccess,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun changeAge(delta: Int) {
        viewModelScope.launch {
            val current = settingsRepository.age.first() ?: SetupUiState.DEFAULT_AGE
            settingsRepository.setAge((current + delta).coerceIn(SetupUiState.MIN_AGE, SetupUiState.MAX_AGE))
        }
    }

    fun selectPreset(preset: ZonePreset) {
        viewModelScope.launch { settingsRepository.setZonePreset(preset) }
    }

    fun selectWarmup(warmup: WarmupDuration) {
        viewModelScope.launch { settingsRepository.setWarmupDuration(warmup) }
    }

    fun selectHapticMode(mode: HapticMode) {
        viewModelScope.launch { settingsRepository.setHapticMode(mode) }
    }

    fun refreshNotificationAccess() {
        notificationAccess.value = mediaController.hasNotificationAccess()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
