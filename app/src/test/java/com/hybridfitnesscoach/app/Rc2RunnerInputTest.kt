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
}
