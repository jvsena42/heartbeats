package com.heartbeats.domain.model

/** Inclusive heart-rate range, in beats per minute, for a target zone. */
data class ZoneRange(val lowBpm: Int, val highBpm: Int) {
    init {
        require(lowBpm in 1..highBpm) { "Invalid zone range: $lowBpm..$highBpm" }
    }

    operator fun contains(bpm: Int): Boolean = bpm in lowBpm..highBpm
}
