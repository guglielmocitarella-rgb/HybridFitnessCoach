package com.hybridfitnesscoach.app

import org.junit.Assert.*
import org.junit.Test

class WorkoutInfrastructureTest {
    @Test fun repsPrescriptionIsDataDriven() {
        val ex=Exercise("Pull-up","Dorso","Sbarra","Intermedio",role="Forza",measure=ExerciseMeasure.REPS,targetSets=3,targetValue=8.0)
        val p=prescriptionFor(ex)
        assertEquals(3,p.sets); assertEquals(8.0,p.value,0.0); assertEquals("rip",p.unit)
    }
    @Test fun isometricPrescriptionUsesSeconds() {
        val ex=Exercise("Front lever progression","Dorso / Core","Sbarra","Intermedio",measure=ExerciseMeasure.SECONDS,targetSets=3,targetValue=20.0)
        val p=prescriptionFor(ex)
        assertEquals(20.0,p.value,0.0); assertEquals("sec",p.unit)
    }
    @Test fun distancePrescriptionUsesMeters() {
        val ex=Exercise("Carry","Core","Manubri","Intermedio",measure=ExerciseMeasure.DISTANCE,targetValue=40.0)
        assertEquals("m",prescriptionFor(ex).unit)
    }
    @Test fun timerReconstructsFromAbsoluteDeadline() {
        assertEquals(30,remainingSeconds(130000L,100000L))
        assertEquals(0,remainingSeconds(90000L,100000L))
    }
    @Test fun partialSessionLoadsRecoveryByActualSets() {
        val now=1_000_000_000L
        val partial=SessionRecord(now,"Test",2,3,listOf("Squat"),completedByExercise=mapOf("Squat" to 2))
        val none=SessionRecord(now,"Test",0,3,listOf("Squat"),completedByExercise=mapOf("Squat" to 0))
        assertTrue((recoveryScores(listOf(partial),now)["Gambe"]?:100)<100)
        assertEquals(100,recoveryScores(listOf(none),now)["Gambe"])
    }
    @Test fun workoutStatePreservesPinnedSkippedAndPartialData() {
        val s=WorkoutExerciseState("Pull-up",ExerciseMeasure.REPS,listOf(WorkoutSetState(0,targetReps=8,actualReps=7,completed=true)),WorkoutExerciseStatus.PARTIAL,rpe=8,pinned=true)
        assertTrue(s.pinned); assertEquals(WorkoutExerciseStatus.PARTIAL,s.status); assertEquals(7,s.sets.first().actualReps)
    }
    @Test fun healthNormalizationIsSeparatedFromWorkoutDecision() {
        val n=normalizeHealth(RawHealthSnapshot(1000L,sleepMinutes=480,restingHeartRateBpm=50.0,activeMinutes=60))
        assertEquals(100.0,n.sleepScore!!,0.0)
        assertEquals(100.0,n.cardiovascularScore!!,0.0)
        assertEquals(100.0,n.activityLoadScore!!,0.0)
    }
}
