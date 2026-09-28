package com.hybridfitnesscoach.app
import org.junit.Assert.*
import org.junit.Test
class WorkoutRunnerEngineTest {
 private fun state()=WorkoutSessionState("x",1,"P",listOf(
  WorkoutExerciseState("A",sets=listOf(WorkoutSetState(0,targetReps=8))),
  WorkoutExerciseState("B",sets=listOf(WorkoutSetState(0,targetSeconds=20))),
  WorkoutExerciseState("C",sets=listOf(WorkoutSetState(0,targetReps=6)))))
 @Test fun skipIsNotFailureAndAdvances(){val s=WorkoutRunnerEngine.skip(state(),0);assertEquals(WorkoutExerciseStatus.SKIPPED,s.exercises[0].status);assertEquals(1,s.currentExerciseIndex)}
 @Test fun skippedExerciseCanBeResumed(){val s=WorkoutRunnerEngine.unskip(WorkoutRunnerEngine.skip(state(),0),0);assertEquals(WorkoutExerciseStatus.PENDING,s.exercises[0].status);assertEquals(0,s.currentExerciseIndex)}
 @Test fun addSetPreservesTarget(){val s=WorkoutRunnerEngine.addSet(state(),0);assertEquals(2,s.exercises[0].sets.size);assertEquals(8,s.exercises[0].sets[1].targetReps)}
 @Test fun reorderPreservesCurrentExerciseIdentity(){val s=WorkoutRunnerEngine.move(state().copy(currentExerciseIndex=1),1,0);assertEquals("B",s.exercises.first().exerciseName);assertEquals(0,s.currentExerciseIndex)}

 @Test fun repsSetStoresExecutedValue(){val s=WorkoutRunnerEngine.completeSet(state(),0,0,actualReps=7);assertEquals(7,s.exercises[0].sets[0].actualReps);assertTrue(s.exercises[0].sets[0].completed)}
 @Test fun isometricSetStoresSeconds(){val s=WorkoutRunnerEngine.completeSet(state(),1,0,actualSeconds=18);assertEquals(18,s.exercises[1].sets[0].actualSeconds)}
 @Test fun universalSetStoresDistanceLoadAndAssistance(){val base=state().copy(exercises=listOf(WorkoutExerciseState("D",measure=ExerciseMeasure.DISTANCE,sets=listOf(WorkoutSetState(0,targetDistanceMeters=100.0,targetLoadKg=20.0,targetAssistanceKg=5.0)))));val s=WorkoutRunnerEngine.completeSet(base,0,0,actualDistanceMeters=90.0,actualLoadKg=22.5,actualAssistanceKg=4.0);assertEquals(90.0,s.exercises[0].sets[0].actualDistanceMeters!!,0.0);assertEquals(22.5,s.exercises[0].sets[0].actualLoadKg!!,0.0);assertEquals(4.0,s.exercises[0].sets[0].actualAssistanceKg!!,0.0)}
 @Test fun partialSessionRemainsPartial(){val base=state().copy(exercises=listOf(WorkoutExerciseState("A",sets=listOf(WorkoutSetState(0,targetReps=8),WorkoutSetState(1,targetReps=8),WorkoutSetState(2,targetReps=8)))));val s=WorkoutRunnerEngine.completeSet(base,0,0,actualReps=8);assertEquals(WorkoutExerciseStatus.PARTIAL,s.exercises[0].status);assertEquals(1,s.exercises[0].sets.count{it.completed})}

    @Test fun elapsedTimeExcludesPausedWallClockTime() {
        val state=WorkoutSessionState("w",1_000L,"P",emptyList(),elapsedBeforePauseMs=4_000L,pausedAtEpochMs=10_000L)
        assertEquals(4,elapsedWorkoutSeconds(state,20_000L))
    }

    @Test fun elapsedTimeResumesFromPersistedPauseBaseline() {
        val state=WorkoutSessionState("w",10_000L,"P",emptyList(),elapsedBeforePauseMs=4_000L)
        assertEquals(7,elapsedWorkoutSeconds(state,13_000L))
    }
}
