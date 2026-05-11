package com.heartbeats.domain.usecase

import com.heartbeats.domain.model.ZoneRange
import com.heartbeats.domain.model.ZoneStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoneStatusClassifierTest {

    private val classifier = ZoneStatusClassifier()
    private val range = ZoneRange(lowBpm = 120, highBpm = 150)

    @Test
    fun `below the lower bound is BELOW`() {
        assertEquals(ZoneStatus.BELOW, classifier.classify(bpm = 119, range = range))
        assertEquals(ZoneStatus.BELOW, classifier.classify(bpm = 40, range = range))
    }

    @Test
    fun `lower bound is inclusive and counts as IN_ZONE`() {
        assertEquals(ZoneStatus.IN_ZONE, classifier.classify(bpm = 120, range = range))
    }

    @Test
    fun `inside the range is IN_ZONE`() {
        assertEquals(ZoneStatus.IN_ZONE, classifier.classify(bpm = 135, range = range))
    }

    @Test
    fun `upper bound is inclusive and counts as IN_ZONE`() {
        assertEquals(ZoneStatus.IN_ZONE, classifier.classify(bpm = 150, range = range))
    }

    @Test
    fun `above the upper bound is ABOVE`() {
        assertEquals(ZoneStatus.ABOVE, classifier.classify(bpm = 151, range = range))
        assertEquals(ZoneStatus.ABOVE, classifier.classify(bpm = 200, range = range))
    }
}
