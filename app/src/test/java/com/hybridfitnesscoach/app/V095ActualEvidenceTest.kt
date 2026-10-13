package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class V095ActualEvidenceTest {
    private fun state(measure:ExerciseMeasure=ExerciseMeasure.REPS):WorkoutSessionState {
        val ex=Exercise("Evidence","Core","Corpo libero","Principiante",measure=measure)
        return WorkoutSessionState("evidence",1000,"Workout",listOf(plannedExerciseState(0,ex).copy(
            rpe=5,sets=listOf(WorkoutSetState(0,targetReps=8.takeIf{measure==ExerciseMeasure.REPS},targetSeconds=30.takeIf{measure==ExerciseMeasure.SECONDS},targetLoadKg=20.0,targetAssistanceKg=5.0)))))
    }
    @Test fun missingActualPerformanceCannotCompleteASet() {
        val before=state(ExerciseMeasure.SECONDS)
        assertEquals(before,WorkoutRunnerEngine.completeSet(before,0,0))
    }
    @Test fun omittedActualLoadAndAssistanceStayUnknown() {
        val done=WorkoutRunnerEngine.completeSet(state(),0,0,actualReps=6)
        val set=done.exercises.single().sets.single()
        assertEquals(6,set.actualReps)
        assertNull(set.actualLoadKg)
        assertNull(set.actualAssistanceKg)
        assertNull(set.actualSeconds)
    }
    @Test fun unexecutedExercisesHaveNoActualEffortInHistory() {
        val record=runnerSessionRecord(state(),2000)
        assertTrue(record.exerciseRpe.isEmpty())
        assertNull(record.structuredExercises.single().rpe)
        assertFalse(historyExerciseDetails(record).single().lines.any{it=="RPE esercizio: 5"})
    }
    @Test fun reopeningASetClearsItsActualEvidence() {
        val completed=WorkoutRunnerEngine.completeSet(state(),0,0,actualReps=6,actualLoadKg=10.0)
        val reopened=WorkoutRunnerEngine.reopenSet(completed,0,0).exercises.single().sets.single()
        assertFalse(reopened.completed)
        assertNull(reopened.actualReps)
        assertNull(reopened.actualLoadKg)
        assertEquals(8,reopened.targetReps)
    }
    @Test fun performanceEffortDoesNotRetroactivelyChangeEarlierSets() {
        val before=state().let{s->s.copy(exercises=s.exercises.map{it.copy(sets=listOf(WorkoutSetState(0,targetReps=8),WorkoutSetState(1,targetReps=8)))})}
        val first=WorkoutRunnerEngine.completeSet(before,0,0,actualReps=8)
        val changed=first.copy(exercises=first.exercises.map{it.copy(rpe=9)})
        val second=WorkoutRunnerEngine.completeSet(changed,0,1,actualReps=8)
        assertEquals(7,runnerSessionRecord(second,2000).structuredExercises.single().rpe)
    }
    @Test fun unperformedOccurrenceDoesNotEraseTheLastObservedLoad() {
        val before=state()
        val previous=runnerSessionRecord(WorkoutRunnerEngine.completeSet(before,0,0,actualReps=8,actualLoadKg=10.0),2000)
        val pending=runnerSessionRecord(before,3000)
        val recommendation=loadRecommendationForExercise(before.exercises.single().exerciseSnapshot!!,listOf(pending,previous),100)
        assertEquals(10.5,recommendation.loadKg!!,0.0)
    }
}
