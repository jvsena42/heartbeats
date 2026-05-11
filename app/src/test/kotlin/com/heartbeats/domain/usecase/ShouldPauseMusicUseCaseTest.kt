package com.heartbeats.domain.usecase

import com.heartbeats.domain.model.ZoneStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldPauseMusicUseCaseTest {

    private fun newUseCase() = ShouldPauseMusicUseCase(belowDebounceMs = 10_000L, resumeDebounceMs = 5_000L)

    private fun below(warmup: Boolean = false) = ZoneSample(bpm = 80, status = ZoneStatus.BELOW, isWarmup = warmup)
    private fun inZone(warmup: Boolean = false) = ZoneSample(bpm = 130, status = ZoneStatus.IN_ZONE, isWarmup = warmup)
    private fun above(warmup: Boolean = false) = ZoneSample(bpm = 190, status = ZoneStatus.ABOVE, isWarmup = warmup)
    private val unavailable = ZoneSample(bpm = null, status = null, isWarmup = false)

    @Test
    fun `in zone never pauses`() {
        val uc = newUseCase()
        assertFalse(uc.shouldPauseMusic(inZone(), nowMs = 0))
        assertFalse(uc.shouldPauseMusic(inZone(), nowMs = 10_000))
        assertFalse(uc.shouldPauseMusic(inZone(), nowMs = 60_000))
        assertFalse(uc.isPaused)
    }

    @Test
    fun `brief dip below the zone shorter than the debounce window does not pause`() {
        val uc = newUseCase()
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 0))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 5_000))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 9_999))
        assertFalse(uc.shouldPauseMusic(inZone(), nowMs = 10_500))
        assertFalse(uc.isPaused)
    }

    @Test
    fun `below the zone for the full debounce window pauses`() {
        val uc = newUseCase()
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 0))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 9_000))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 9_999))
        assertTrue(uc.shouldPauseMusic(below(), nowMs = 10_000))
        assertTrue(uc.isPaused)
    }

    @Test
    fun `once paused, recovery to zone for the full resume window resumes`() {
        val uc = newUseCase()
        uc.shouldPauseMusic(below(), nowMs = 0)
        assertTrue(uc.shouldPauseMusic(below(), nowMs = 10_000))
        assertTrue(uc.shouldPauseMusic(inZone(), nowMs = 10_500))
        assertTrue(uc.shouldPauseMusic(inZone(), nowMs = 15_000))
        assertTrue(uc.shouldPauseMusic(inZone(), nowMs = 15_400))
        assertFalse(uc.shouldPauseMusic(inZone(), nowMs = 15_500))
        assertFalse(uc.isPaused)
    }

    @Test
    fun `a blip back into the zone restarts the below debounce`() {
        val uc = newUseCase()
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 0))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 8_000))
        assertFalse(uc.shouldPauseMusic(inZone(), nowMs = 8_500))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 9_000))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 18_000))
        assertTrue(uc.shouldPauseMusic(below(), nowMs = 19_000))
    }

    @Test
    fun `above the zone never pauses`() {
        val uc = newUseCase()
        assertFalse(uc.shouldPauseMusic(above(), nowMs = 0))
        assertFalse(uc.shouldPauseMusic(above(), nowMs = 30_000))
        assertFalse(uc.shouldPauseMusic(above(), nowMs = 120_000))
        assertFalse(uc.isPaused)
    }

    @Test
    fun `above the zone does not count toward the resume window`() {
        val uc = newUseCase()
        uc.shouldPauseMusic(below(), nowMs = 0)
        assertTrue(uc.shouldPauseMusic(below(), nowMs = 10_000))
        assertTrue(uc.shouldPauseMusic(above(), nowMs = 11_000))
        assertTrue(uc.shouldPauseMusic(above(), nowMs = 30_000))
        assertTrue(uc.shouldPauseMusic(inZone(), nowMs = 31_000))
        assertTrue(uc.shouldPauseMusic(inZone(), nowMs = 35_999))
        assertFalse(uc.shouldPauseMusic(inZone(), nowMs = 36_000))
    }

    @Test
    fun `does not pause during warmup even when below the zone`() {
        val uc = newUseCase()
        assertFalse(uc.shouldPauseMusic(below(warmup = true), nowMs = 0))
        assertFalse(uc.shouldPauseMusic(below(warmup = true), nowMs = 10_000))
        assertFalse(uc.shouldPauseMusic(below(warmup = true), nowMs = 60_000))
        assertFalse(uc.isPaused)
        // when warmup ends, the below debounce starts fresh
        assertFalse(uc.shouldPauseMusic(below(warmup = false), nowMs = 61_000))
        assertFalse(uc.shouldPauseMusic(below(warmup = false), nowMs = 70_999))
        assertTrue(uc.shouldPauseMusic(below(warmup = false), nowMs = 71_000))
    }

    @Test
    fun `null bpm never causes a pause`() {
        val uc = newUseCase()
        assertFalse(uc.shouldPauseMusic(unavailable, nowMs = 0))
        assertFalse(uc.shouldPauseMusic(unavailable, nowMs = 10_000))
        assertFalse(uc.shouldPauseMusic(unavailable, nowMs = 60_000))
        assertFalse(uc.isPaused)
    }

    @Test
    fun `a sensor dropout does not accumulate toward a pause`() {
        val uc = newUseCase()
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 0))
        assertFalse(uc.shouldPauseMusic(unavailable, nowMs = 5_000))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 9_000))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 15_000))
        assertTrue(uc.shouldPauseMusic(below(), nowMs = 19_000))
    }

    @Test
    fun `reset clears state`() {
        val uc = newUseCase()
        uc.shouldPauseMusic(below(), nowMs = 0)
        assertTrue(uc.shouldPauseMusic(below(), nowMs = 10_000))
        uc.reset()
        assertFalse(uc.isPaused)
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 11_000))
        assertFalse(uc.shouldPauseMusic(below(), nowMs = 20_999))
        assertTrue(uc.shouldPauseMusic(below(), nowMs = 21_000))
    }
}
