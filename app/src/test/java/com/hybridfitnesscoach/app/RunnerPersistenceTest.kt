package com.hybridfitnesscoach.app
import org.junit.Assert.*
import org.junit.Test

class RunnerPersistenceTest {
    @Test fun partialSkippedExerciseKeepsActualSetsAndPin() {
        val sets=listOf(WorkoutSetState(0,actualReps=3,completed=true),WorkoutSetState(1,targetReps=6))
        val saved=recordedExerciseState(0,exercises.first(),sets,9,true,true)
        assertEquals(WorkoutExerciseStatus.SKIPPED,saved.status)
        assertEquals(1,saved.sets.count{it.completed})
        assertEquals(3,saved.sets.first().actualReps)
        assertTrue(saved.pinned)
    }
    @Test fun missingExecutionCannotBecomeCompleted() {
        val saved=recordedExerciseState(0,exercises.first(),null,null,false,false)
        assertEquals(0,saved.sets.count{it.completed})
        assertEquals(WorkoutExerciseStatus.PENDING,saved.status)
    }
    @Test fun pinnedRegenerationDoesNotApplyDeloadTwice() {
        val context=coachingContext(emptyList(),25)
        val originals=applyCoaching(exercises.take(3),context)
        val pinned=setOf(originals[1].name)
        val result=regenerateCoachedWorkout(originals,exercises.drop(3).take(5),pinned,context)
        assertEquals(originals[1],result[1])
        val again=regenerateCoachedWorkout(result,exercises.drop(3).take(5),pinned,context)
        assertEquals(originals[1],again[1])
    }
    @Test fun advanceChangesBodyweightPrescription() {
        val ex=Exercise("Push-up","Petto","Corpo libero","Principiante",targetValue=10.0)
        val plan=adaptiveWorkoutPlan(1.0,7.0,100,100)
        assertEquals(ProgressionDecision.ADVANCE,plan.decision)
        val adapted=applyCoaching(listOf(ex),CoachingContext(plan,emptyMap(),100,2)).single()
        assertTrue(prescriptionFor(adapted).value>10.0)
    }
}
