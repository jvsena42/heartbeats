package com.heartbeats.domain.model

/** UI-facing state of a Heartbeats session. */
sealed interface WorkoutState {

    data object Idle : WorkoutState

    /**
     * Warmup period: heart rate is streamed and shown, but zone enforcement is OFF.
     * [inZone] lets the UI offer a "skip warmup" button (we never auto-skip).
     */
    data class Warmup(
        val remainingSeconds: Int,
        val currentBpm: Int?,
        val inZone: Boolean,
        val hrAvailable: Boolean = true,
    ) : WorkoutState

    /** Active enforcement: zone is monitored and music is paused/resumed accordingly. */
    data class Active(
        val currentBpm: Int?,
        val zoneStatus: ZoneStatus,
        val hrAvailable: Boolean = true,
        val musicPaused: Boolean = false,
    ) : WorkoutState
}
