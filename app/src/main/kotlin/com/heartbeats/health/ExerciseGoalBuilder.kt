package com.heartbeats.health

import androidx.health.services.client.data.ComparisonType
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeCondition
import androidx.health.services.client.data.ExerciseGoal

/**
 * Builds the exercise goal Heartbeats registers once warmup ends: a one-time goal
 * that fires when the heart-rate statistic drops to/below the bottom of the target
 * zone. The goal is only a battery-efficient wake-up trigger — the authoritative,
 * debounced pause/resume decision lives in
 * [com.heartbeats.domain.usecase.ShouldPauseMusicUseCase].
 */
internal object ExerciseGoalBuilder {

    fun belowZoneGoal(lowBpm: Int): ExerciseGoal<Double> {
        val condition = DataTypeCondition(
            dataType = DataType.HEART_RATE_BPM_STATS,
            threshold = lowBpm.toDouble(),
            comparisonType = ComparisonType.LESS_THAN_OR_EQUAL,
        )
        return ExerciseGoal.createOneTimeGoal(condition)
    }
}
