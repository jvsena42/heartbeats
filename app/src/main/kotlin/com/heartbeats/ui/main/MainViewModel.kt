package com.heartbeats.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heartbeats.data.repository.SettingsRepository
import com.heartbeats.domain.model.WorkoutState
import com.heartbeats.domain.model.ZonePreset
import com.heartbeats.domain.model.ZoneRange
import com.heartbeats.domain.usecase.ComputeZoneRangeUseCase
import com.heartbeats.media.MediaControllerManager
import com.heartbeats.service.WorkoutController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val workout: WorkoutState = WorkoutState.Idle,
    val zoneRange: ZoneRange? = null,
    val preset: ZonePreset = ZonePreset.DEFAULT,
    val notificationAccessGranted: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val workoutController: WorkoutController,
    private val mediaController: MediaControllerManager,
    settingsRepository: SettingsRepository,
    private val computeZoneRange: ComputeZoneRangeUseCase,
) : ViewModel() {

    private val notificationAccess = MutableStateFlow(mediaController.hasNotificationAccess())

    val uiState: StateFlow<MainUiState> = combine(
        workoutController.state,
        settingsRepository.age,
        settingsRepository.zonePreset,
        notificationAccess,
    ) { workout, age, preset, hasAccess ->
        MainUiState(
            workout = workout,
            zoneRange = age?.let { computeZoneRange(it, preset) },
            preset = preset,
            notificationAccessGranted = hasAccess,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MainUiState())

    fun startWorkout() = workoutController.start()

    fun stopWorkout() = workoutController.stop()

    fun skipWarmup() = workoutController.skipWarmup()

    fun refreshNotificationAccess() {
        notificationAccess.value = mediaController.hasNotificationAccess()
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
