package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class V095TimedRunnerTest {
    private fun state():WorkoutSessionState {
        val ex=Exercise("Hold","Core","Corpo libero","Principiante",measure=ExerciseMeasure.SECONDS,targetSets=2,targetValue=30.0)
        return WorkoutSessionState("clock",1000,"Workout",listOf(plannedExerciseState(0,ex)))
    }
    @Test fun preparationDoesNotCountAsExecutedDuration() {
        val s=TimedRunner.start(state(),1000)
        assertEquals(5,TimedRunner.preparationSeconds(s,1000))
        assertEquals(0,TimedRunner.executedSeconds(s,5000))
        assertEquals(1,TimedRunner.executedSeconds(s,7000))
        assertFalse(s.exercises.single().sets.first().completed)
    }
    @Test fun stoppingDuringPreparationRecordsNoPerformance() {
        val stopped=TimedRunner.stop(TimedRunner.start(state(),1000),4000)
        assertNull(stopped.timedSet)
        assertEquals(0,runnerSessionRecord(stopped,4000).completedSets)
        assertTrue(runnerSessionRecord(stopped,4000).exerciseRpe.isEmpty())
    }
    @Test fun earlyStopRecordsOnlyElapsedPerformance() {
        val stopped=TimedRunner.stop(TimedRunner.start(state(),1000),10000)
        val actual=stopped.exercises.single().sets.first()
        assertEquals(4,actual.actualSeconds)
        assertEquals(30,actual.targetSeconds)
        assertEquals(6000L,actual.actualStartedAtEpochMs)
        assertEquals(10000L,actual.actualFinishedAtEpochMs)
    }
    @Test fun pauseResumeExcludesBackgroundPauseFromActualDuration() {
        val paused=WorkoutRunnerEngine.pause(TimedRunner.start(state(),1000),8000)
        assertEquals(2,TimedRunner.executedSeconds(paused,99000))
        val resumed=WorkoutRunnerEngine.resume(paused,20000)
        assertEquals(3,TimedRunner.executedSeconds(resumed,21000))
    }
    @Test fun shortRestPreparationDoesNotAddFiveSeconds() {
        val s=TimedRunner.start(state().copy(restStartedAtEpochMs=1000,restEndsAtEpochMs=4000),1000,afterRest=true)
        assertEquals(3,TimedRunner.preparationSeconds(s,1000))
        assertEquals(1,TimedRunner.executedSeconds(s,5000))
    }
    @Test fun expiryAfterBackgroundUsesThePersistedClock() {
        val s=TimedRunner.tick(TimedRunner.start(state(),1000),90000)
        assertEquals(30,s.exercises.single().sets.first().actualSeconds)
        assertEquals(36000L,s.exercises.single().sets.first().actualFinishedAtEpochMs)
    }
    @Test fun saveWhilePausedKeepsOnlyPrePausePerformance() {
        val paused=WorkoutRunnerEngine.pause(TimedRunner.start(state(),1000),10000)
        val stopped=TimedRunner.stop(paused,90000)
        assertEquals(4,stopped.exercises.single().sets.first().actualSeconds)
        assertEquals(10000L,stopped.exercises.single().sets.first().actualFinishedAtEpochMs)
        assertEquals(10000L,stopped.pausedAtEpochMs)
    }
    @Test fun invalidLoadDoesNotStartClock() {
        assertNull(TimedRunner.start(state(),1000,loadKg=-1.0).timedSet)
        assertNull(TimedRunner.start(state(),1000,assistanceKg=Double.NaN).timedSet)
    }
    @Test fun skippingRestWhilePausedStartsAtResumeWithoutAnotherDelay() {
        val resting=TimedRunner.start(state().copy(restStartedAtEpochMs=1000,restEndsAtEpochMs=11000),1000,afterRest=true)
        val paused=WorkoutRunnerEngine.pause(resting,3000)
        val skipped=skipRunnerRest(paused,90000)
        val resumed=WorkoutRunnerEngine.resume(skipped,100000)
        assertEquals(100000L,resumed.timedSet!!.startsAtEpochMs)
        assertEquals(1,TimedRunner.executedSeconds(resumed,101000))
    }
    @Test fun skippingTimedOccurrenceCannotRecordItsAutomaticallyArmedNextSet() {
        val stopped=TimedRunner.stop(TimedRunner.start(state(),1000),10000)
        val skipped=WorkoutRunnerEngine.skip(stopped,0)
        assertNull(skipped.timedSet)
        val later=TimedRunner.tick(skipped,999000)
        assertEquals(WorkoutExerciseStatus.SKIPPED,later.exercises.single().status)
        assertFalse(later.exercises.single().sets[1].completed)
    }
    @Test fun autoArmedTimedSetCanPersistItsOwnObservedResistance() {
        val s=TimedRunner.stop(TimedRunner.start(state(),1000,loadKg=10.0),10000)
        val entered=TimedRunner.observedResistance(s,0,12.0,null)
        val reloaded=entered
        val stopped=TimedRunner.stop(reloaded,reloaded.timedSet!!.startsAtEpochMs+3000)
        assertEquals(12.0,stopped.exercises.single().sets[1].actualLoadKg!!,0.0)
        assertEquals(10.0,stopped.exercises.single().sets[0].actualLoadKg!!,0.0)
    }
    @Test fun cancelPreparationWhilePausedPreservesFrozenRecovery() {
        val resting=TimedRunner.start(state().copy(restStartedAtEpochMs=1000,restEndsAtEpochMs=6000),1000,afterRest=true)
        val paused=WorkoutRunnerEngine.pause(resting,3000)
        val stopped=TimedRunner.stop(paused,90000)
        assertEquals(3000L,stopped.pausedAtEpochMs)
        assertEquals(3,stopped.pausedRestRemainingSec)
        assertNull(stopped.timedSet)
        assertFalse(stopped.exercises.single().sets.first().completed)
    }
    @Test fun timerIsBoundToOccurrenceAndCannotFollowNavigation() {
        val s=state().let{it.copy(exercises=it.exercises+it.exercises.single())}
        val started=TimedRunner.start(s,1000)
        val selected=WorkoutRunnerEngine.select(started,1)
        val stopped=TimedRunner.stop(selected,10000)
        assertEquals(4,stopped.exercises[0].sets[0].actualSeconds)
        assertFalse(stopped.exercises[1].sets[0].completed)
    }
}
