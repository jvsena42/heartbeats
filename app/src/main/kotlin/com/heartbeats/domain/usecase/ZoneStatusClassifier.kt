package com.heartbeats.domain.usecase

import com.heartbeats.domain.model.ZoneRange
import com.heartbeats.domain.model.ZoneStatus
import javax.inject.Inject

/** Maps a BPM reading to [ZoneStatus]. Boundaries are inclusive: `bpm == lowBpm` is [ZoneStatus.IN_ZONE]. */
class ZoneStatusClassifier @Inject constructor() {

    fun classify(bpm: Int, range: ZoneRange): ZoneStatus = when {
        bpm < range.lowBpm -> ZoneStatus.BELOW
        bpm > range.highBpm -> ZoneStatus.ABOVE
        else -> ZoneStatus.IN_ZONE
    }
}
