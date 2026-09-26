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
}
