package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class UniversalRunnerLifecycleTest {
    private fun state():WorkoutSessionState {
        val ex=Exercise("Same", "Petto", "Corpo libero", "Principiante",targetSets=12,targetValue=90.0)
        return WorkoutSessionState("s",1000,"Home",listOf(
            plannedExerciseState(0,ex).copy(sets=listOf(WorkoutSetState(0,targetReps=4)),setRestSec=17,rpe=5,pinned=true),
            plannedExerciseState(1,ex).copy(sets=listOf(WorkoutSetState(0,targetReps=7),WorkoutSetState(1,targetReps=8)),rpe=8)
        ),plannedWorkoutId="owner",trainingLocationId="home")
    }
    @Test fun undoAfterCompletionClearsActualsAndKeepsAllTargets() {
        val base=state()
        val complete=WorkoutRunnerEngine.completeSet(base,0,0,actualReps=3,actualLoadKg=21.0)
        val undo=WorkoutRunnerEngine.undo(complete,0)
        assertEquals(base.exercises[0].sets,undo.exercises[0].sets)
        assertEquals(base.exercises[1],undo.exercises[1])
        assertTrue(undo.exercises[0].pinned)
        assertEquals(0,undo.currentExerciseIndex)
    }
    @Test fun pauseResumePreservesElapsedTimeAndRestAcrossProcessRecreation() {
        val base=state().copy(restEndsAtEpochMs=41000)
        val paused=WorkoutRunnerEngine.pause(base,11000)
        assertEquals(30,paused.pausedRestRemainingSec)
        assertEquals(10,elapsedWorkoutSeconds(paused,99000))
        val resumed=WorkoutRunnerEngine.resume(paused,101000)
        assertEquals(131000L,resumed.restEndsAtEpochMs)
        assertEquals(15,elapsedWorkoutSeconds(resumed,106000))
        assertEquals(base.exercises,resumed.exercises)
    }
    @Test fun historyAggregatesNamesakesWithoutDroppingStructuredOccurrences() {
        val done=WorkoutRunnerEngine.completeSet(WorkoutRunnerEngine.completeSet(state(),0,0,actualReps=3),1,0,actualReps=6)
        val record=runnerSessionRecord(done,9000)
        assertEquals(2,record.structuredExercises.size)
        assertEquals(2,record.completedByExercise["Same"])
        assertEquals(2,record.completedSets)
        assertEquals(3,record.totalSets)
        assertEquals("owner",record.plannedWorkoutId)
        assertEquals("home",record.trainingLocationId)
    }
    @Test fun allOriginsKeepSnapshotThroughPreviewRunnerAndHistory() {
        WorkoutOrigin.entries.forEach { origin ->
            val intent=WorkoutLaunchIntent(origin,sourceExerciseId=if(origin==WorkoutOrigin.SINGLE_EXERCISE)"same" else null,plannedWorkoutId=if(origin==WorkoutOrigin.PLAN)"owner" else null)
            val snapshot=state().copy(plannedWorkoutId=intent.plannedWorkoutId)
            val flow=materializeWorkoutLaunch(selectWorkoutLocation(beginWorkoutLaunch(intent),"home"),snapshot)
            val runner=requireNotNull(startWorkoutRunner(flow).workout)
            assertEquals(snapshot.exercises,runner.exercises)
            assertEquals(origin.name,runner.workoutOrigin)
            assertEquals(origin.name,runnerSessionRecord(runner,9000).workoutOrigin)
            assertEquals(snapshot.plannedWorkoutId,runner.plannedWorkoutId)
            assertEquals(runner,resumeWorkoutLaunch(runner).workout)
        }
    }
    @Test fun undoSkipResumeKeepTheSameOccurrenceAndActualEvidence() {
        val completed=WorkoutRunnerEngine.completeSet(state(),1,0,actualReps=3)
        val skipped=WorkoutRunnerEngine.skip(completed,1)
        val resumed=WorkoutRunnerEngine.unskip(skipped,1)
        assertEquals(completed.exercises[1].sets,resumed.exercises[1].sets)
        assertEquals(WorkoutExerciseStatus.PARTIAL,resumed.exercises[1].status)
        assertEquals(state().exercises[0],resumed.exercises[0])
    }
    @Test fun customDurationBudgetIsDeterministicAndNeverSelectsBeyondBudget() {
        val candidate=state().exercises.first().exerciseSnapshot!!
        val result=fitCustomWorkoutDuration(listOf(candidate,candidate.copy(name="Next")),15)
        assertEquals(result,fitCustomWorkoutDuration(listOf(candidate,candidate.copy(name="Next")),15))
        assertTrue(result.isNotEmpty())
        assertTrue(result.sumOf(::estimatedExerciseSeconds)<=15*60)
    }
    @Test fun regenerationRetainsPinnedPrescriptionWithinTheSameDurationBudget() {
        val pinned=Exercise("Pinned","Petto","Corpo libero","Principiante",targetSets=1,targetValue=6.0)
        val full=Exercise("Replacement","Petto","Corpo libero","Principiante",targetSets=12,targetValue=90.0)
        val result=fitCustomWorkoutDuration(listOf(pinned,full),15,setOf(pinned.name))
        assertEquals(pinned,result.first())
        assertTrue(result.sumOf(::estimatedExerciseSeconds)<=15*60)
    }
    @Test fun historyDetailAndRecoveryUseEveryExecutedOccurrence() {
        val snapshot=state()
        val done=WorkoutRunnerEngine.completeSet(snapshot,1,0,actualReps=6)
        val record=runnerSessionRecord(done,9000)
        val details=historyExerciseDetails(record)
        assertEquals(2,details.size)
        assertEquals(0,details[0].completedSets)
        assertEquals(1,details[1].completedSets)
        assertEquals(95,recoveryScores(listOf(record),9000)["Petto"])
    }
    @Test fun pinnedSkillPreservesItsDurationTrimmedSnapshotOnRegeneration() {
        val skill="Muscle-up"
        val node=SkillCatalog.forSkill(skill).first{it.id=="pullup_strict"}
        val pinned=exercises.first{ex->(node.exerciseAliases+node.exerciseName).any{it.equals(ex.name,true)}}.copy(targetSets=1)
        val regenerated=regenerateSkillWorkout(listOf(pinned),listOf(pinned),setOf(pinned.name),coachingContext(emptyList(),100),listOf(skill),emptyList())
        assertEquals(pinned,regenerated.single())
    }
    @Test fun freezeRetainsDifferentPerSetTargetsAndExplicitLoadAndRest() {
        val source=state().copy(exercises=listOf(state().exercises.first().copy(
            exerciseSnapshot=state().exercises.first().exerciseSnapshot!!.copy(resistanceMode=ResistanceMode.EXTERNAL_LOAD),
            sets=listOf(WorkoutSetState(0,targetReps=4,targetLoadKg=10.0),WorkoutSetState(1,targetReps=7,targetLoadKg=12.0))
        )))
        val frozen=freezeWorkoutTargets(source,emptyList(),10)
        assertEquals(source.exercises,frozen.exercises)
        val runner=startWorkoutRunner(materializeWorkoutLaunch(selectWorkoutLocation(beginWorkoutLaunch(WorkoutLaunchIntent(WorkoutOrigin.PLAN,plannedWorkoutId="owner")),"home"),frozen))
        assertEquals(frozen.exercises,runner.workout?.exercises)
    }
    @Test fun materializationDefaultsRpeToFiveWithoutChangingPrescriptions() {
        val source=state().copy(exercises=state().exercises.map{it.copy(rpe=null)})
        val frozen=freezeWorkoutTargets(source,emptyList(),100)
        assertTrue(frozen.exercises.all{it.rpe==5})
        assertEquals(source.exercises.map{it.sets},frozen.exercises.map{it.sets})
        assertTrue(runnerSessionRecord(frozen,9000).structuredExercises.all{it.rpe==5})
    }
}
