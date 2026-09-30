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
    @Test fun plannedWorkoutLaunchPreservesMaterializedPlanPrescription() {
        val planned=Exercise("Planned press","Petto","Bilanciere","Intermedio",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD,targetSets=4,targetValue=9.0,setRestSec=137)
        val athlete=AthleteProfile(goal="Forza",level="Intermedio",assessedTrainingExperience=2,assessedPushCapacity=2,assessedPullCapacity=2)
        val launch=effectiveAutomaticExercises(listOf(planned),athlete,emptyList(),20,restored=true).single()
        assertEquals(planned,launch)
        assertEquals(4,launch.targetSets)
        assertEquals(9.0,launch.targetValue!!,0.001)
        assertEquals(137,launch.setRestSec)
    }

    @Test fun freshSuggestedWorkoutStillMaterializesCurrentAdaptivePrescription() {
        val source=Exercise("Fresh press","Petto","Bilanciere","Intermedio",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD,targetSets=4,targetValue=9.0,setRestSec=137)
        val athlete=AthleteProfile(goal="Forza",level="Intermedio",assessedTrainingExperience=2,assessedPushCapacity=2,assessedPullCapacity=2)
        val fresh=effectiveAutomaticExercises(listOf(source),athlete,emptyList(),20,restored=false).single()
        assertNotEquals(source,fresh)
    }

    @Test fun undoLastSetClearsOnlyNewestCompletedExecutionAndAllowsRetry() {
        val sets=listOf(
            WorkoutSetState(0,targetReps=8,actualReps=8,actualLoadKg=80.0,completed=true,restUsedSec=90),
            WorkoutSetState(1,targetReps=8,actualReps=7,actualLoadKg=80.0,completed=true,restUsedSec=90),
            WorkoutSetState(2,targetReps=8)
        )
        val undone=undoLastCompletedSet(sets)
        assertTrue(undone[0].completed)
        assertEquals(8,undone[0].actualReps)
        assertFalse(undone[1].completed)
        assertNull(undone[1].actualReps)
        assertNull(undone[1].actualLoadKg)
        assertNull(undone[1].restUsedSec)
        assertEquals(1,undone.count{it.completed})
        assertEquals(sets[2],undone[2])
    }

    @Test fun undoWithNoCompletedSetIsNoOp() {
        val sets=listOf(WorkoutSetState(0,targetReps=8),WorkoutSetState(1,targetReps=8))
        assertEquals(sets,undoLastCompletedSet(sets))
    }

}
