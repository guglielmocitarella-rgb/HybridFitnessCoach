package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class AdaptiveRegressionTest {
    private val bench = Exercise("Bench", "Petto", "Bilanciere", "Intermedio", resistanceMode=ResistanceMode.EXTERNAL_LOAD)
    private fun attempt(load:Double=100.0, reps:Int=8, rpe:Int?=7) = WorkoutExerciseState("Bench", sets=(0..2).map {
        WorkoutSetState(it, targetReps=8, actualReps=reps, actualLoadKg=load, completed=true)
    }, rpe=rpe)
    private fun session(time:Long, ex:WorkoutExerciseState)=SessionRecord(time,"P",3,3,structuredExercises=listOf(ex))

    @Test fun newestTimestampDeterminesLoadRegardlessOfInputOrder() {
        val history=listOf(session(1,attempt(50.0)),session(2,attempt(100.0)))
        assertEquals(105.0,loadRecommendationForExercise(bench,history,85).loadKg!!,0.0)
    }
    @Test fun completedSetsWithMissedRepsDoNotIncreaseLoad() {
        assertTrue(loadRecommendationForExercise(bench,listOf(session(1,attempt(reps=3))),85).loadKg!!<100.0)
    }
    @Test fun unknownRpeDoesNotInventProgressionEvidence() {
        assertEquals(100.0,loadRecommendationForExercise(bench,listOf(session(1,attempt(rpe=null))),85).loadKg!!,0.0)
    }
    @Test fun duplicateAttemptsInOneSessionCannotMasterSkill() {
        val node=SkillNode("bench","Bench","Strength",criterion=MasteryCriterion(minReps=8))
        val one=session(1,attempt()).copy(structuredExercises=listOf(attempt(),attempt()))
        assertEquals(ProgressionStatus.IN_PROGRESS,ProgressionEngine.evaluate(node,emptySet(),listOf(one)).status)
    }
    @Test fun regenerationCannotDuplicateFallbackExercise() {
        val a=bench.copy(name="A");val b=bench.copy(name="B")
        val result=regenerateUnpinned(listOf(a,b),listOf(b),emptySet())
        assertEquals(result.size,result.map{it.name}.distinct().size)
    }
}
