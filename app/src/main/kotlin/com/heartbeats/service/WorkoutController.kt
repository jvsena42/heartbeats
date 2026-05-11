package com.heartbeats.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.heartbeats.domain.model.WorkoutState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide handle to the workout session. The UI observes [state] and issues
 * [start] / [stop] / [skipWarmup]; [WorkoutService] is the producer that publishes
 * state and consumes the skip signal.
 */
@Singleton
class WorkoutController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow<WorkoutState>(WorkoutState.Idle)
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private val _skipWarmup = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    internal val skipWarmupSignal: Flow<Unit> = _skipWarmup.asSharedFlow()

    internal fun publish(state: WorkoutState) {
        _state.value = state
    }

    fun start() {
        ContextCompat.startForegroundService(context, intent(WorkoutService.ACTION_START))
    }

    fun stop() {
        runCatching { context.startService(intent(WorkoutService.ACTION_STOP)) }
    }

    fun skipWarmup() {
        _skipWarmup.tryEmit(Unit)
    }

    private fun intent(action: String): Intent =
        Intent(context, WorkoutService::class.java).setAction(action)
}
