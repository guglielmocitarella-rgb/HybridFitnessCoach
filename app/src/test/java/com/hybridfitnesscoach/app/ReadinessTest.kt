package com.hybridfitnesscoach.app
import org.junit.Assert.*
import org.junit.Test

class ReadinessTest {
    @Test fun missingSignalsRemainMissing() {
        val state=DefaultHealthNormalizer.normalize(RawHealthSnapshot(100))
        assertNull(state.sleepScore)
        assertNull(state.cardiovascularRecoveryScore)
        assertNull(DefaultReadinessProvider(100).readiness(AthleteReadinessInput(emptyMap(),state)))
    }
    @Test fun shortSleepReducesReadinessAndActualVolume() {
        val health=DefaultHealthNormalizer.normalize(RawHealthSnapshot(100,sleepMinutes=180))
        val score=DefaultReadinessProvider(100).readiness(AthleteReadinessInput(mapOf("Core" to 100),health))!!
        assertTrue(score<45)
        assertEquals(ProgressionDecision.DELOAD,coachingContext(emptyList(),score.toInt()).plan.decision)
    }
    @Test fun staleAndInvalidReadingsDoNotAffectReadiness() {
        val state=NormalizedHealthState(1,sleepScore=10.0)
        assertNull(DefaultReadinessProvider(200_000_000).readiness(AthleteReadinessInput(emptyMap(),state)))
        val invalid=DefaultHealthNormalizer.normalize(RawHealthSnapshot(1,sleepMinutes=-2,restingHeartRateBpm=Double.NaN))
        assertNull(invalid.sleepScore)
        assertNull(invalid.cardiovascularRecoveryScore)
    }
    @Test fun cardiovascularSignalsNeedPersonalBaseline() {
        assertNull(DefaultHealthNormalizer.normalize(RawHealthSnapshot(1,restingHeartRateBpm=80.0,hrvMs=25.0)).cardiovascularRecoveryScore)
        val stressed=DefaultHealthNormalizer.normalize(RawHealthSnapshot(1,restingHeartRateBpm=80.0,hrvMs=25.0,baselineRestingHeartRateBpm=60.0,baselineHrvMs=50.0))
        assertTrue(stressed.cardiovascularRecoveryScore!!<60)
    }
    @Test fun overlappingSleepRecordsAreNotDoubleCounted() {
        assertEquals(180,mergedIntervalMinutes(listOf(0L to 7_200_000L,3_600_000L to 10_800_000L)))
    }
}
