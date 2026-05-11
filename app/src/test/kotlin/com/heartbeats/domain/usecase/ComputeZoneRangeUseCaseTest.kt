package com.heartbeats.domain.usecase

import com.heartbeats.domain.model.ZonePreset
import com.heartbeats.domain.model.ZoneRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComputeZoneRangeUseCaseTest {

    private val useCase = ComputeZoneRangeUseCase()

    @Test
    fun `age 20 produces expected ranges for every preset`() {
        // maxHr = 200
        assertEquals(ZoneRange(100, 140), useCase(age = 20, preset = ZonePreset.FAT_BURN))
        assertEquals(ZoneRange(140, 170), useCase(age = 20, preset = ZonePreset.CARDIO))
        assertEquals(ZoneRange(170, 190), useCase(age = 20, preset = ZonePreset.PEAK))
    }

    @Test
    fun `age 35 produces expected ranges for every preset`() {
        // maxHr = 185; ties round up (92.5 -> 93, 129.5 -> 130)
        assertEquals(ZoneRange(93, 130), useCase(age = 35, preset = ZonePreset.FAT_BURN))
        assertEquals(ZoneRange(130, 157), useCase(age = 35, preset = ZonePreset.CARDIO))
        assertEquals(ZoneRange(157, 176), useCase(age = 35, preset = ZonePreset.PEAK))
    }

    @Test
    fun `age 50 produces expected ranges for every preset`() {
        // maxHr = 170; 144.5 -> 145
        assertEquals(ZoneRange(85, 119), useCase(age = 50, preset = ZonePreset.FAT_BURN))
        assertEquals(ZoneRange(119, 145), useCase(age = 50, preset = ZonePreset.CARDIO))
        assertEquals(ZoneRange(145, 162), useCase(age = 50, preset = ZonePreset.PEAK))
    }

    @Test
    fun `age 70 produces expected ranges for every preset`() {
        // maxHr = 150; 127.5 -> 128, 142.5 -> 143
        assertEquals(ZoneRange(75, 105), useCase(age = 70, preset = ZonePreset.FAT_BURN))
        assertEquals(ZoneRange(105, 128), useCase(age = 70, preset = ZonePreset.CARDIO))
        assertEquals(ZoneRange(128, 143), useCase(age = 70, preset = ZonePreset.PEAK))
    }

    @Test
    fun `max heart rate is 220 minus age`() {
        assertEquals(200, useCase.maxHeartRate(20))
        assertEquals(185, useCase.maxHeartRate(35))
        assertEquals(150, useCase.maxHeartRate(70))
    }

    @Test
    fun `invalid ages return null`() {
        assertNull(useCase(age = 0, preset = ZonePreset.CARDIO))
        assertNull(useCase(age = -10, preset = ZonePreset.CARDIO))
        assertNull(useCase(age = 121, preset = ZonePreset.CARDIO))
        assertNull(useCase(age = 999, preset = ZonePreset.CARDIO))
        assertNull(useCase.maxHeartRate(0))
        assertNull(useCase.maxHeartRate(121))
    }

    @Test
    fun `boundary ages are valid`() {
        assertEquals(ZoneRange(110, 153), useCase(age = 1, preset = ZonePreset.FAT_BURN))
        assertEquals(ZoneRange(50, 70), useCase(age = 120, preset = ZonePreset.FAT_BURN))
    }
}
