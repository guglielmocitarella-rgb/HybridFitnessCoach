package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class UniversalRunnerSnapshotTest {
    private fun repeated():WorkoutSessionState {
        val ex=Exercise("Repeated", "Petto", "Corpo libero", "Principiante",targetSets=9,targetValue=99.0)
        return WorkoutSessionState("repeat",1000,"Home",listOf(
            plannedExerciseState(0,ex).copy(sets=listOf(WorkoutSetState(0,targetReps=4)),setRestSec=17),
            plannedExerciseState(1,ex).copy(sets=listOf(WorkoutSetState(0,targetReps=7,targetLoadKg=12.0),WorkoutSetState(1,targetReps=8)),setRestSec=29)
        ),currentExerciseIndex=1,trainingLocationId="home",plannedWorkoutId="plan-owner")
    }
    @Test fun moveKeepsTheSelectedOccurrenceWhenNamesAndCatalogIdsRepeat() {
        val moved=WorkoutRunnerEngine.move(repeated().copy(currentExerciseIndex=0),0,1)
        assertEquals(1,moved.currentExerciseIndex)
        assertEquals(4,moved.exercises[moved.currentExerciseIndex].sets.first().targetReps)
    }
    @Test fun completingOneOccurrenceDoesNotAlterItsNamesakeOrSnapshot() {
        val before=repeated()
        val after=WorkoutRunnerEngine.completeSet(before,1,0,actualReps=6,actualLoadKg=10.0)
        assertEquals(before.exercises[0],after.exercises[0])
        assertEquals(7,after.exercises[1].sets[0].targetReps)
        assertEquals(8,after.exercises[1].sets[1].targetReps)
        assertEquals(6,after.exercises[1].sets[0].actualReps)
        assertEquals(before.plannedWorkoutId,after.plannedWorkoutId)
        assertEquals(before.trainingLocationId,after.trainingLocationId)
    }
    @Test fun startRejectsAnEmptyPreview() {
        val flow=beginWorkoutLaunch(WorkoutLaunchIntent(WorkoutOrigin.ADAPTIVE_EXTRA))
        val preview=materializeWorkoutLaunch(selectWorkoutLocation(flow,"home"),repeated().copy(exercises=emptyList(),plannedWorkoutId=null))
        assertThrows(IllegalArgumentException::class.java){startWorkoutRunner(preview)}
    }
}
