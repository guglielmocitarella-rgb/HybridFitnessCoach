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
    @Test fun absentSetUsesTheSamePrescriptionAsPreviewAndPersistence() {
        val ex = exercises.first { it.name == "Australian pull-up" }.copy(targetSets = 3, targetValue = 10.0)
        val planned = plannedExerciseState(0, ex)
        assertEquals(10.0, runnerInputTarget(ex, null), 0.0)
        assertTrue(planned.sets.all { it.targetReps == 10 })
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

    @Test fun assessedFitnessLevelBoundariesAreDeterministic() {
        assertEquals("Principiante", assessedFitnessLevel(FitnessLevelAssessment(trainingMonths=2,pushUps=10,pullUps=1)))
        assertEquals("Intermedio", assessedFitnessLevel(FitnessLevelAssessment(trainingMonths=6,pushUps=15,pullUps=3)))
        assertEquals("Avanzato", assessedFitnessLevel(FitnessLevelAssessment(trainingMonths=24,pushUps=30,pullUps=10)))
    }

    @Test fun assessedLevelActuallyConstrainsGeneratedWorkoutDifficulty() {
        val gym=profiles.first{it.name=="Palestra"}
        listOf(
            FitnessLevelAssessment(2,10,1) to "Principiante",
            FitnessLevelAssessment(6,15,3) to "Intermedio",
            FitnessLevelAssessment(24,30,10) to "Avanzato"
        ).forEach { (assessment, expected) ->
            val level=assessedFitnessLevel(assessment)
            assertEquals(expected,level)
            val workout=generateWorkout(gym,AthleteProfile(goal="Fitness generale",level=level),emptyList(),80)
            assertTrue(workout.isNotEmpty())
            assertTrue(workout.all{levelScore(it.level)<=levelScore(level)})
        }
    }

}
