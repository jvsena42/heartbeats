package com.heartbeats.domain.model

/** Which zone transitions trigger haptic feedback. */
enum class HapticMode {
    OFF,
    BELOW_ONLY,
    BOTH,
    ;

    companion object {
        val DEFAULT: HapticMode = BOTH
    }
}
