package com.heartbeats.domain.model

/**
 * Heart-rate training zones, expressed as a fraction of the user's estimated
 * maximum heart rate (220 − age).
 */
enum class ZonePreset(val lowPct: Float, val highPct: Float) {
    FAT_BURN(0.50f, 0.70f),
    CARDIO(0.70f, 0.85f),
    PEAK(0.85f, 0.95f),
    ;

    companion object {
        val DEFAULT: ZonePreset = FAT_BURN
    }
}
