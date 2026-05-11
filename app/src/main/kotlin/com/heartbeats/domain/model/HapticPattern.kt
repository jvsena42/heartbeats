package com.heartbeats.domain.model

/**
 * Vibration waveforms (off/on millisecond pairs starting with an initial delay)
 * used for the differentiated haptics described in the product spec.
 */
enum class HapticPattern(val timings: LongArray) {
    /** Below zone — slow double-buzz. Repeated every 5s while below until pause/recovery. */
    BELOW_ZONE(longArrayOf(0, 200, 100, 200)),

    /** Above zone — rapid triple-buzz, warning only (music keeps playing). */
    ABOVE_ZONE(longArrayOf(0, 80, 60, 80, 60, 80)),

    /** Back in zone — single gentle pulse. */
    BACK_IN_ZONE(longArrayOf(0, 150)),

    /** Warmup: 30 seconds remaining. */
    WARMUP_THIRTY_SECONDS(longArrayOf(0, 120)),

    /** Warmup: 10 seconds remaining. */
    WARMUP_TEN_SECONDS(longArrayOf(0, 120, 80, 120)),

    /** Warmup complete — enforcement begins. */
    WARMUP_COMPLETE(longArrayOf(0, 250, 120, 250, 120, 250)),
}
