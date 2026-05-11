package com.heartbeats.domain.usecase

import com.heartbeats.domain.model.ZoneStatus
import javax.inject.Inject

/** A single heart-rate observation fed to [ShouldPauseMusicUseCase]. */
data class ZoneSample(
    val bpm: Int?,
    val status: ZoneStatus?,
    val isWarmup: Boolean,
)

/**
 * Decides whether music should currently be paused, applying debounce windows so a
 * brief dip below the zone doesn't immediately cut the music and a brief recovery
 * doesn't resume it prematurely. This mirrors (and is the authoritative version of)
 * the debounced Health Services exercise goal used as a battery-efficient trigger.
 *
 * Stateful classifier: feed samples in order with a monotonically increasing [nowMs]
 * (e.g. `SystemClock.elapsedRealtime()`); each call returns the desired paused state.
 *
 * Rules:
 *  - Never *newly* pauses during warmup, or when the sensor is unavailable (bpm == null).
 *  - "Above zone" is a haptic warning only — it never pauses music.
 *  - Pauses once heart rate has been BELOW the zone for [belowDebounceMs].
 *  - Resumes once heart rate has been back IN_ZONE for [resumeDebounceMs].
 */
class ShouldPauseMusicUseCase @Inject constructor(
    private val belowDebounceMs: Long = DEFAULT_BELOW_DEBOUNCE_MS,
    private val resumeDebounceMs: Long = DEFAULT_RESUME_DEBOUNCE_MS,
) {
    private var belowSinceMs: Long? = null
    private var inZoneSinceMs: Long? = null
    private var paused: Boolean = false

    val isPaused: Boolean get() = paused

    fun reset() {
        belowSinceMs = null
        inZoneSinceMs = null
        paused = false
    }

    fun shouldPauseMusic(sample: ZoneSample, nowMs: Long): Boolean {
        val status = sample.status
        if (sample.isWarmup || sample.bpm == null || status == null) {
            // No enforcement: don't accumulate toward a pause; leave existing state untouched.
            belowSinceMs = null
            return paused
        }

        when (status) {
            ZoneStatus.BELOW -> {
                inZoneSinceMs = null
                val since = belowSinceMs ?: nowMs.also { belowSinceMs = it }
                if (nowMs - since >= belowDebounceMs) {
                    paused = true
                }
            }

            ZoneStatus.ABOVE -> {
                // Warning-only territory: never pauses, and isn't "in zone" so it doesn't
                // count toward resuming. Keep the current paused state; clear both timers.
                belowSinceMs = null
                inZoneSinceMs = null
            }

            ZoneStatus.IN_ZONE -> {
                belowSinceMs = null
                if (paused) {
                    val since = inZoneSinceMs ?: nowMs.also { inZoneSinceMs = it }
                    if (nowMs - since >= resumeDebounceMs) {
                        paused = false
                        inZoneSinceMs = null
                    }
                } else {
                    inZoneSinceMs = null
                }
            }
        }
        return paused
    }

    companion object {
        const val DEFAULT_BELOW_DEBOUNCE_MS = 10_000L
        const val DEFAULT_RESUME_DEBOUNCE_MS = 5_000L
    }
}
