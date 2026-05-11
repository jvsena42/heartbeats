package com.heartbeats.domain.usecase

import com.heartbeats.domain.model.ZonePreset
import com.heartbeats.domain.model.ZoneRange
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Converts an age + [ZonePreset] into a concrete BPM range using the classic
 * `maxHr = 220 - age` estimate. Returns `null` for ages outside [MIN_AGE]..[MAX_AGE].
 */
class ComputeZoneRangeUseCase @Inject constructor() {

    operator fun invoke(age: Int, preset: ZonePreset): ZoneRange? {
        val maxHr = maxHeartRate(age) ?: return null
        val low = (maxHr * preset.lowPct).roundToInt()
        val high = (maxHr * preset.highPct).roundToInt()
        return ZoneRange(lowBpm = low, highBpm = high)
    }

    fun maxHeartRate(age: Int): Int? =
        if (age in MIN_AGE..MAX_AGE) MAX_HR_CONSTANT - age else null

    companion object {
        const val MAX_HR_CONSTANT = 220
        const val MIN_AGE = 1
        const val MAX_AGE = 120
    }
}
