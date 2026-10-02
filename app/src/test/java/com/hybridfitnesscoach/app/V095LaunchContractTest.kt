package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class V095LaunchContractTest {
    @Test fun singleExerciseSkipsOnlyLocationAndKeepsTheCommonPreview() {
        val intent=WorkoutLaunchIntent(WorkoutOrigin.SINGLE_EXERCISE,sourceExerciseId="selected")
        val start=beginWorkoutLaunch(intent)
        assertEquals(WorkoutLaunchStage.MATERIALIZE,start.stage)
        assertNull(start.locationId)
        val ex=Exercise("Selected","Petto","Bilanciere","Avanzato")
        val preview=materializeWorkoutLaunch(start,singleExerciseWorkoutState(ex,"Singolo esercizio",1000))
        val runner=startWorkoutRunner(preview).workout!!
        assertEquals("Selected",runner.exercises.single().exerciseName)
        assertEquals(WorkoutOrigin.SINGLE_EXERCISE.name,runner.workoutOrigin)
        assertNull(runner.trainingLocationId)
    }
    @Test fun singleExerciseCanContinueAfterAllSuggestedSets() {
        val ex=Exercise("Selected","Petto","Corpo libero","Principiante",targetSets=1)
        var state=singleExerciseWorkoutState(ex,"Singolo esercizio",1000)
        repeat(12){i->
            state=WorkoutRunnerEngine.completeSet(state,0,i,actualReps=6)
            assertEquals(i+1,state.exercises.single().sets.count{it.completed})
            state=WorkoutRunnerEngine.addSet(state,0)
        }
        assertEquals(13,state.exercises.single().sets.size)
        assertNull(state.exercises.single().sets.last().actualRpe)
    }
}
