package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class Rc2RunnerInputTest {
    @Test fun firstSetUsesItsPrescribedRepsInsteadOfRecalculatingExerciseTarget() {
        val ex = exercises.first { it.name == "Australian pull-up" }.copy(targetValue = 9.0)
        assertEquals(10.0, runnerInputTarget(ex, WorkoutSetState(0, targetReps = 10)), 0.0)
    }
    @Test fun restoredTimedAndDistanceSetsUsePersistedTargets() {
        val ex = exercises.first().copy(measure = ExerciseMeasure.SECONDS, targetValue = 30.0)
        assertEquals(25.0, runnerInputTarget(ex, WorkoutSetState(0, targetSeconds = 25)), 0.0)
        assertEquals(400.0, runnerInputTarget(ex.copy(measure = ExerciseMeasure.DISTANCE), WorkoutSetState(0, targetDistanceMeters = 400.0)), 0.0)
    }
    @Test fun runnerRequiresAnAuthoritativeMaterializedTarget() {
val ex=exercises.first{it.name=="Australian pull-up"}.copy(targetValue=10.0)
 val planned=plannedExerciseState(0,ex)
 assertTrue(planned.sets.all{runnerInputTarget(ex,it)==10.0})
 assertThrows(IllegalArgumentException::class.java){runnerInputTarget(ex,WorkoutSetState(0))}
 }

    @Test fun newRunnerEffortDefaultsToFiveButPreservesExplicitValues() {
        assertEquals(5, runnerEffort(null))
        assertEquals(7, runnerEffort(7))
    }

    @Test fun outOfRangeRunnerEffortFallsBackToFive() {
        assertEquals(5, runnerEffort(0))
        assertEquals(5, runnerEffort(11))
    }
    @Test fun singleExerciseLaunchBuildsRunnableWorkoutStateWithDefaultEffort() {
        val ex=Exercise("Test execute","Core","Corpo libero","Intermedio",targetSets=4,targetValue=8.0)
        val state=singleExerciseWorkoutState(ex,"Palestra",startedAtEpochMs=1234L)
        assertEquals("single",state.runnerKind)
        assertEquals("Palestra",state.profileName)
        assertEquals(1,state.exercises.size)
        assertEquals("Test execute",state.exercises.single().exerciseName)
        assertEquals(4,state.exercises.single().sets.size)
        assertEquals(5,state.exercises.single().rpe)
        assertEquals("Test execute",state.exercises.single().exerciseSnapshot?.name)
    }

    @Test fun singleExerciseLaunchUsesExercisePrescriptionForTimeTargets() {
        val ex=Exercise("Hold","Core","Corpo libero","Intermedio",measure=ExerciseMeasure.SECONDS,targetSets=3,targetValue=20.0)
        val state=singleExerciseWorkoutState(ex,"Casa",startedAtEpochMs=9L)
        assertEquals(listOf(20,20,20),state.exercises.single().sets.map{it.targetSeconds})
        assertTrue(state.exercises.single().sets.all{it.targetReps==null})
    }

    @Test fun assessedDimensionsAreDeterministic() {
CapabilityDimension.entries.forEach{d->(1..3).forEach{level->
 assertEquals(level,AthleteProfile().withCapability(d,level).toCapabilityProfile().levelFor(d))
 }}
 }

    @Test fun assessedDimensionsConstrainGeneratedDifficulty() {
val gym=trainingLocations.first{it.name=="Palestra"}
 (1..3).forEach{level->
 var athlete=AthleteProfile(goal="Fitness generale")
 CapabilityDimension.entries.forEach{athlete=athlete.withCapability(it,level)}
 val workout=generateWorkout(gym,athlete,emptyList(),80)
 assertTrue(workout.isNotEmpty())
 assertTrue(workout.all{levelScore(it.level)<=capabilityLevelFor(it,athlete.toCapabilityProfile())})
 }
 }

    @Test fun directExecuteUsesTheSameEffectivePrescriptionAsAutomaticRunner() {
        val ex=Exercise("Direct contract press","Petto","Manubri","Intermedio",
            role="Forza",category=ExerciseCategory.PUSH,resistanceMode=ResistanceMode.EXTERNAL_LOAD,
            setRestSec=90,exerciseRestSec=120,targetSets=3,targetValue=6.0)
        val athlete=AthleteProfile(assessedTrainingExperience=2,assessedPushCapacity=2,assessedPullCapacity=2,goal="Forza")
        val effective=exerciseWithEffectivePrescription(ex,athlete,emptyList(),80)
        val state=singleExerciseWorkoutState(ex,"Palestra",1234L,athlete,emptyList(),80)
        val launched=state.exercises.single()
        assertEquals(effective.targetSets,launched.sets.size)
        assertTrue(launched.sets.all{it.targetReps==effective.targetValue?.toInt()})
        assertEquals(effective.setRestSec,launched.setRestSec)
        assertEquals(effective.exerciseRestSec,launched.exerciseRestSec)
        assertEquals(effective.setRestSec,launched.exerciseSnapshot?.setRestSec)
    }

}
